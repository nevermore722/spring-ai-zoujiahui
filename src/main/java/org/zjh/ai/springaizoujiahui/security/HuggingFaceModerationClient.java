package org.zjh.ai.springaizoujiahui.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional third-party moderation via HuggingFace Inference API.
 * <p>
 * This requires a HuggingFace token with "Inference Providers" permission.
 */
public class HuggingFaceModerationClient {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceModerationClient.class);

    public record LabeledScore(String label, double score) {}

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceModerationClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    public List<LabeledScore> classify(String modelId, String hfToken, String text) {
        try {
            URI uri = URI.create("https://api-inference.huggingface.co/models/" + modelId);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(25))
                    .header("Authorization", "Bearer " + hfToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"inputs\":" + quoteJson(text) + "}",
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("HuggingFace moderation failed: status={}, body={}", resp.statusCode(), trim(resp.body(), 400));
                return List.of();
            }

            String body = resp.body();
            JsonNode root = objectMapper.readTree(body);

            // Typical response: Array of objects: [{label, score}, ...]
            if (root.isArray()) {
                List<LabeledScore> out = new ArrayList<>();
                for (JsonNode item : root) {
                    if (item == null) continue;
                    JsonNode labelNode = item.get("label");
                    JsonNode scoreNode = item.get("score");
                    if (labelNode == null || scoreNode == null) continue;
                    out.add(new LabeledScore(labelNode.asText(), scoreNode.asDouble()));
                }
                return out;
            }

            // Some models may return an object with an "error"
            if (root.has("error")) {
                log.warn("HuggingFace moderation returned error: {}", root.get("error").asText());
                return List.of();
            }

            return List.of();
        } catch (Exception e) {
            log.warn("HuggingFace moderation exception: {}", e.getMessage());
            return List.of();
        }
    }

    private static String quoteJson(String s) {
        // Minimal JSON string escaping.
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}

