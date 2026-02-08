package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 朋友聊天
 */
@Slf4j
@Service("generalAI")
public class GeneralAI {
    // 智能对话的客户端
    private final ChatClient chatClient;

    private static final String BOYFRIEND_SYSTEM_MESSAGE = "你需要解决用户的各种问题";

    @Autowired
    public GeneralAI(ChatClient.Builder chatClientBuilder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }

    public Flux<String> deepseek(String message, String chatId) {
        log.info("chatId:{},message:{}", chatId, message);
        Flux<String> content = this.chatClient.prompt()
                .user(message)
                .system(BOYFRIEND_SYSTEM_MESSAGE)
                // 2. 通过 chatMemorySpec 指定当前对话的 ID (用于区分不同用户或会话)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content().share();
        content.collectList()
                .map(list -> String.join("", list))
                .subscribe(fullContent -> log.info("本次对话完整回答: {}", fullContent));
        return content;
    }
}
