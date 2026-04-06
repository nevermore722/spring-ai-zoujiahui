package org.zjh.ai.springaizoujiahui.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.zjh.ai.springaizoujiahui.security.SensitiveContentValidator;
import org.zjh.ai.springaizoujiahui.service.FortuneTelling;
import org.zjh.ai.springaizoujiahui.service.FriendsChat;
import org.zjh.ai.springaizoujiahui.service.GeneralAI;
import org.zjh.ai.springaizoujiahui.service.SixteenPersonalities;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI应用构造器
 */
@RestController
@RequestMapping("ai/app")
@Slf4j
public class AIApplicationController {
    private final FortuneTelling fortuneTelling;

    private final FriendsChat friendsChat;

    private final GeneralAI generalAI;

    private final SixteenPersonalities sixteenPersonalities;

    private final SensitiveContentValidator sensitiveContentValidator;

    public AIApplicationController(
            FortuneTelling fortuneTelling,
            FriendsChat friendsChat,
            GeneralAI generalAI,
            SixteenPersonalities sixteenPersonalities,
            SensitiveContentValidator sensitiveContentValidator
    ) {
        this.fortuneTelling = fortuneTelling;
        this.friendsChat = friendsChat;
        this.generalAI = generalAI;
        this.sixteenPersonalities = sixteenPersonalities;
        this.sensitiveContentValidator = sensitiveContentValidator;
    }

    @GetMapping(value = "/fortuneTelling", produces = "text/html;charset=UTF-8")
    public Flux<String> streamHistory(@RequestParam(value = "message", defaultValue = "我要算命") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return fortuneTelling.chat(message, chatId);
    }

    @GetMapping(value = "/boyfriend", produces = "text/html;charset=UTF-8")
    public Flux<String> boyfriend(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return friendsChat.boyfriend(message, chatId);
    }

    @GetMapping(value = "/boyfriend/rag", produces = "text/html;charset=UTF-8")
    public Flux<String> chatWithBoyfriendRag(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return friendsChat.boyfriendWithRag(message, chatId);
    }

    @GetMapping(value = "/girlfriend", produces = "text/html;charset=UTF-8")
    public Flux<String> girlfriend(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return friendsChat.girlfriend(message, chatId);
    }

    @GetMapping(value = "/girlfriend/rag", produces = "text/html;charset=UTF-8")
    public Flux<String> chatWithGirlfriendRag(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return friendsChat.girlfriendWithRag(message, chatId);
    }

    @GetMapping(value = "/deepseek", produces = "text/html;charset=UTF-8")
    public Flux<String> deepseek(@RequestParam(value = "message", defaultValue = "你好") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return generalAI.deepseek(message, chatId);
    }

    @GetMapping(value = "/startTesting", produces = "text/html;charset=UTF-8")
    public Flux<String> startTesting(@RequestParam(value = "message", defaultValue = "开始测试") String message, @RequestParam String chatId) {
        sensitiveContentValidator.validateOrThrow(message);
        return sixteenPersonalities.startTesting(message, chatId);
    }

    /**
     * 清除指定会话
     */
    @DeleteMapping("/{chatId}")
    public Map<String, Object> clearSession(@PathVariable String chatId) {
        friendsChat.clearSession(chatId);
        return Map.of("success", true, "message", "会话已清除: " + chatId);
    }

    /**
     * 清除所有过期会话
     */
    @PostMapping("/clean-expired")
    public Map<String, Object> cleanExpired() {
        int count = friendsChat.clearExpiredSessions();
        return Map.of("success", true, "cleanedCount", count);
    }

    /**
     * 获取所有活跃会话
     */
    @GetMapping("/active")
    public Map<String, Object> getActiveSessions() {
        var sessions = friendsChat.getActiveSessions();
        return Map.of(
                "success", true,
                "count", sessions.size(),
                "sessions", sessions
        );
    }
}
