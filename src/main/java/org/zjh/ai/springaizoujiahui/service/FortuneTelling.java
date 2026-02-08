package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

/**
 * AI算命
 */
@Slf4j
@Service("fortuneTelling")
public class FortuneTelling {
    // 智能对话的客户端
    private final ChatClient chatClient;

    private static final String SYSTEM_MESSAGE = "你是一个算命专家，可以算西方的塔罗星座也能算东方的生辰八字，你需要一步一步引导用户问你问题进行算命，先问对方叫什么名字，提供几个算命方向并表明自己什么都能算，一次只问一个问题，但可以多问几步，最终返回算命结果";

    @Autowired
    public FortuneTelling(ChatClient.Builder chatClientBuilder) {
        chatClientBuilder.defaultSystem(SYSTEM_MESSAGE);
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(50)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }

    public Flux<String> chat(String message, String chatId) {
        log.info("chatId:{},message:{}", chatId, message);
        Flux<String> content = this.chatClient.prompt()
                .user(message)
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
