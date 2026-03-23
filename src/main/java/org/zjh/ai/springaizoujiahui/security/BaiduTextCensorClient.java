package org.zjh.ai.springaizoujiahui.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Baidu Cloud "Content Review Platform - Text" client.
 * <p>
 * Enable by providing credentials via environment variables:
 * - BAIDU_AK
 * - BAIDU_SK
 * Optional:
 * - BAIDU_STRATEGY_ID (int, default omitted)
 * - BAIDU_BLOCK_DOUBTFUL (true/false, default true)
 * - BAIDU_FAIL_OPEN (true/false, default true)
 */
@Slf4j
public class BaiduTextCensorClient {
    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String CENSOR_URL_BASE = "https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String ak;
    private final String sk;

    private final Integer strategyId; // optional
    private final boolean blockDoubtful; // conclusionType==3
    private final boolean failOpen;

    // token cache
    private volatile String accessToken;
    private volatile long accessTokenExpireAtMs; // epoch ms

    public BaiduTextCensorClient(
            ObjectMapper objectMapper,
            String ak,
            String sk,
            Integer strategyId,
            boolean blockDoubtful,
            boolean failOpen
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.ak = ak;
        this.sk = sk;
        this.strategyId = strategyId;
        this.blockDoubtful = blockDoubtful;
        this.failOpen = failOpen;
    }

    public BaiduDecision moderate(String text) {
        try {
            String token = getAccessToken();
            if (token == null || token.isBlank()) {
                return failOpen ? BaiduDecision.pass("baidu_token_empty") : BaiduDecision.block("百度审核失败：token获取失败");
            }

            String body = buildBody(text);
            URI uri = URI.create(CENSOR_URL_BASE + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                String msg = "百度审核失败：http_" + resp.statusCode();
                return failOpen ? BaiduDecision.pass(msg) : BaiduDecision.block(msg);
            }

            JsonNode root = objectMapper.readTree(resp.body());
            int conclusionType = root.path("conclusionType").asInt(-1);
            String conclusion = root.path("conclusion").asText("");
            String matchedMsg = extractMatchedMsg(root);

            // conclusionType: 1 合规, 2 不合规, 3 疑似, 4 审核失败
            if (conclusionType == 1) {
                return BaiduDecision.pass("合规");
            }
            if (conclusionType == 2) {
                return BaiduDecision.block("不合规" + (matchedMsg == null ? "" : ("：" + matchedMsg)));
            }
            if (conclusionType == 3) {
                if (blockDoubtful) {
                    return BaiduDecision.block("疑似不合规" + (matchedMsg == null ? "" : ("：" + matchedMsg)));
                }
                return BaiduDecision.pass("疑似（已放行）" + (matchedMsg == null ? "" : ("：" + matchedMsg)));
            }

            // conclusionType==4 or unknown -> treat as fail
            String msg = "审核失败" + (matchedMsg == null ? "" : ("：" + matchedMsg));
            return failOpen ? BaiduDecision.pass(msg) : BaiduDecision.block(msg);
        } catch (Exception e) {
            log.warn("Baidu moderation exception: {}", e.getMessage());
            return failOpen ? BaiduDecision.pass("baidu_error") : BaiduDecision.block("百度审核失败");
        }
    }

    private String buildBody(String text) {
        // Content-Type: application/x-www-form-urlencoded
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        if (strategyId == null) {
            return "text=" + encodedText;
        }
        return "text=" + encodedText + "&strategyId=" + strategyId;
    }

    private String getAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpireAtMs - 60_000) {
            return accessToken;
        }

        String url = ACCESS_TOKEN_URL
                + "?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(ak, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(sk, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("Baidu token fetch failed: http_{}", resp.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(resp.body());
        String token = root.path("access_token").asText(null);
        int expiresIn = root.path("expires_in").asInt(0);
        if (token != null && !token.isBlank() && expiresIn > 0) {
            accessToken = token;
            accessTokenExpireAtMs = now + expiresIn * 1000L;
            return token;
        }
        return token;
    }

    private static String extractMatchedMsg(JsonNode root) {
        JsonNode data = root.get("data");
        if (data != null && data.isArray() && data.size() > 0) {
            JsonNode first = data.get(0);
            if (first != null) {
                JsonNode msg = first.get("msg");
                if (msg != null && !msg.isNull()) {
                    return msg.asText();
                }
            }
        }
        return null;
    }

    public record BaiduDecision(boolean pass, String message) {
        public static BaiduDecision pass(String message) {
            return new BaiduDecision(true, message);
        }

        public static BaiduDecision block(String message) {
            return new BaiduDecision(false, message);
        }
    }
}

