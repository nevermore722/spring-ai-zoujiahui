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
@Service("friendsChat")
public class FriendsChat {
    // 智能对话的客户端
    private final ChatClient chatClient;

    private static final String BOYFRIEND_SYSTEM_MESSAGE = "你需要像男朋友（对象）一样的与对方进行聊天，如果对方让你先聊你要随机自己找话题，你是一个阳光开朗的大男孩，28岁左右，知识渊博，见识广，会照顾人";

    private static final String GIRLFRIEND_SYSTEM_MESSAGE = "你需要像女朋友（对象）一样的与对方进行聊天，如果对方让你先聊你要随机自己找话题，你是一个古怪机灵的小女孩，28岁左右，善解人意，会体贴人";

    @Autowired
    public FriendsChat(ChatClient.Builder chatClientBuilder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }

    public Flux<String> boyfriend(String message, String chatId) {
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

    public Flux<String> girlfriend(String message, String chatId) {
        log.info("chatId:{},message:{}", chatId, message);
        Flux<String> content = this.chatClient.prompt()
                .user(message)
                .system(GIRLFRIEND_SYSTEM_MESSAGE)
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
