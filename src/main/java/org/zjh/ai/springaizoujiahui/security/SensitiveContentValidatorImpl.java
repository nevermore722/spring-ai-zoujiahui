package org.zjh.ai.springaizoujiahui.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sensitive input validation (block unsafe inputs before calling LLM).
 * <p>
 * Default behavior: local Chinese-oriented rules (PII/secrets) + configurable Chinese keyword list.
 */
@Slf4j
@Service
public class SensitiveContentValidatorImpl implements SensitiveContentValidator {

    // PII patterns (simple but effective for early blocking)
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("\\b1\\d{10}\\b"); // Mainland China mobile (11 digits)
    private static final Pattern ID_CARD = Pattern.compile("\\b\\d{17}[\\dXx]\\b");

    // Bank card: 16-19 digits (we also normalize separators before checking length)
    private static final Pattern BANK_CARD_DIGITS = Pattern.compile("\\d{16,19}");

    private static final Pattern SECRET_KEYWORDS = Pattern.compile(
            "(?i)\\b(api[-_ ]?key|access[-_ ]?token|secret|password|private[-_ ]?key|sk-[a-zA-Z0-9]{8,}|密钥|私钥|密码|token|令牌|apiKey)\\b"
    );

    // Simple heuristic for extremely long numeric sequences (e.g., copied IDs)
    private static final Pattern LONG_NUMERIC_SEQ = Pattern.compile("\\d{13,}");

    private final boolean enabled;
    private final int maxChars;

    private static final String KEYWORDS_RESOURCE = "sensitive/blocked_keywords.txt";
    private final List<String> blockedKeywords;

    private final BaiduTextCensorClient baiduClient; // null when not configured

    public SensitiveContentValidatorImpl(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {

        // Read from environment variables / system properties directly to avoid requiring extra config classes.
        // You can set them via `application.yaml` or JVM env vars.
        this.enabled = Boolean.parseBoolean(System.getProperty("sensitive.enabled",
                System.getenv().getOrDefault("SENSITIVE_ENABLED", "true")));
        this.maxChars = Integer.parseInt(System.getProperty("sensitive.maxChars",
                System.getenv().getOrDefault("SENSITIVE_MAX_CHARS", "2000")));
        this.blockedKeywords = loadBlockedKeywords();

        String ak = System.getProperty("baidu.ak", System.getenv("BAIDU_AK"));
        String sk = System.getProperty("baidu.sk", System.getenv("BAIDU_SK"));

        boolean blockDoubtful = Boolean.parseBoolean(System.getProperty("baidu.blockDoubtful",
                System.getenv().getOrDefault("BAIDU_BLOCK_DOUBTFUL", "true")));
        boolean failOpen = Boolean.parseBoolean(System.getProperty("baidu.failOpen",
                System.getenv().getOrDefault("BAIDU_FAIL_OPEN", "true")));

        Integer strategyId = null;
        String strategyIdStr = System.getProperty("baidu.strategyId", System.getenv("BAIDU_STRATEGY_ID"));
        if (strategyIdStr != null && !strategyIdStr.isBlank()) {
            try {
                strategyId = Integer.parseInt(strategyIdStr.trim());
            } catch (NumberFormatException ignore) {}
        }

        if (ak != null && !ak.isBlank() && sk != null && !sk.isBlank()) {
            this.baiduClient = new BaiduTextCensorClient(objectMapper, ak, sk, strategyId, blockDoubtful, failOpen);
        } else {
            this.baiduClient = null;
        }
    }

    @Override
    public void validateOrThrow(String message) {
        if (!enabled) return;

        if (message == null || message.isBlank()) {
            throw new SensitiveContentException("输入内容为空");
        }
        if (message.length() > maxChars) {
            throw new SensitiveContentException("输入内容过长，请检查后重试");
        }

        // 1) Local fast checks (PII/secrets)
        String localHit = hitLocal(message);
        if (localHit != null) throw new SensitiveContentException("命中敏感信息： " + localHit);

        // 2) Configurable Chinese keyword list
        String keywordHit = hitByKeywords(message);
        if (keywordHit != null) throw new SensitiveContentException("命中敏感内容： " + keywordHit);

        // 3) Optional Baidu third-party moderation (Chinese)
        if (baiduClient != null) {
            BaiduTextCensorClient.BaiduDecision decision = baiduClient.moderate(message);
            if (!decision.pass()) {
                throw new SensitiveContentException("百度审核拦截：" + decision.message());
            }
        }
    }

    private String hitLocal(String message) {
        Matcher mEmail = EMAIL.matcher(message);
        if (mEmail.find()) return "邮箱";

        Matcher mPhone = PHONE.matcher(message);
        if (mPhone.find()) return "手机号";

        Matcher mId = ID_CARD.matcher(message);
        if (mId.find()) return "身份证号";

        // Normalize separators for bank card detection
        String normalizedDigits = message.replaceAll("[\\s-]", "");
        Matcher mCard = BANK_CARD_DIGITS.matcher(normalizedDigits);
        if (mCard.find() && normalizedDigits.length() >= 16) {
            // Keep it stricter to reduce false positives
            String digitsOnly = normalizedDigits.replaceAll("\\D", "");
            if (digitsOnly.length() >= 16 && digitsOnly.length() <= 19) return "银行卡号";
        }

        if (SECRET_KEYWORDS.matcher(message).find()) return "密钥/密码";

        if (LONG_NUMERIC_SEQ.matcher(message).find()) return "疑似长数字串";

        return null;
    }

    private String hitByKeywords(String message) {
        if (blockedKeywords.isEmpty()) return null;
        String normalized = normalizeForMatch(message);
        for (String kw : blockedKeywords) {
            if (kw == null || kw.isBlank()) continue;
            if (normalized.contains(kw)) {
                return kw;
            }
        }
        return null;
    }

    private List<String> loadBlockedKeywords() {
        try {
            InputStream in = SensitiveContentValidatorImpl.class.getClassLoader().getResourceAsStream(KEYWORDS_RESOURCE);
            if (in == null) {
                log.info("No keyword resource found: {} (only regex rules will be used)", KEYWORDS_RESOURCE);
                return List.of();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                return br.lines()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                        .map(line -> {
                            // Supported formats:
                            //  - keyword
                            //  - category|keyword
                            //  - category:keyword
                            int pipeIdx = line.indexOf('|');
                            if (pipeIdx > 0) return line.substring(pipeIdx + 1).trim();
                            int colonIdx = line.indexOf(':');
                            if (colonIdx > 0) return line.substring(colonIdx + 1).trim();
                            return line;
                        })
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.toLowerCase(Locale.ROOT)) // also normalize English variants
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to load blocked keywords: {}", e.getMessage());
            return List.of();
        }
    }

    private String normalizeForMatch(String s) {
        if (s == null) return "";
        // normalize fullwidth punctuation to ASCII variants
        String out = s;
        out = out.replace('（', '(').replace('）', ')')
                .replace('【', '[').replace('】', ']')
                .replace('。', '.').replace('，', ',')
                .replace('：', ':').replace('；', ';')
                .toLowerCase(Locale.ROOT);
        return out;
    }
}

