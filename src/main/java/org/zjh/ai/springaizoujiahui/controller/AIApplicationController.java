package org.zjh.ai.springaizoujiahui.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zjh.ai.springaizoujiahui.service.FortuneTelling;
import org.zjh.ai.springaizoujiahui.service.FriendsChat;
import reactor.core.publisher.Flux;

/**
 * AI应用构造器
 */
@RestController
@RequestMapping("ai/app")
@Slf4j
public class AIApplicationController {
    private final FortuneTelling fortuneTelling;

    private final FriendsChat friendsChat;

    public AIApplicationController(FortuneTelling fortuneTelling, FriendsChat friendsChat) {
        this.fortuneTelling = fortuneTelling;
        this.friendsChat = friendsChat;
    }

    @GetMapping(value = "/fortuneTelling", produces = "text/html;charset=UTF-8")
    public Flux<String> streamHistory(@RequestParam(value = "message", defaultValue = "我要算命") String message, @RequestParam String chatId) {
        return fortuneTelling.chat(message, chatId);
    }

    @GetMapping(value = "/boyfriend", produces = "text/html;charset=UTF-8")
    public Flux<String> boyfriend(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        return friendsChat.boyfriend(message, chatId);
    }

    @GetMapping(value = "/girlfriend", produces = "text/html;charset=UTF-8")
    public Flux<String> girlfriend(@RequestParam(value = "message", defaultValue = "早上好") String message, @RequestParam String chatId) {
        return friendsChat.girlfriend(message, chatId);
    }
}
