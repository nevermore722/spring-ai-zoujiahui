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
 * 16人格测试
 */
@Slf4j
@Service("sixteenPersonalities")
public class SixteenPersonalities {
    // 智能对话的客户端
    private final ChatClient chatClient;

    private static final String SYSTEM_MESSAGE = "你现在是一个16Personalities的测试程序，你需要设计10个问题，每个问题4个选项，来帮助用户测出ta是什么人格，问题需要一个一个问，问问题时不要额外在选项后加请选择这种话。在第10个问题回答后返回测出来的结果，尽量详细一点，用户可以进一步提问让你解释人格。";

    @Autowired
    public SixteenPersonalities(ChatClient.Builder chatClientBuilder) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(100)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }

    public Flux<String> startTesting(String message, String chatId) {
        log.info("chatId:{},message:{}", chatId, message);
        Flux<String> content = this.chatClient.prompt()
                .user(message)
                .system(SYSTEM_MESSAGE)
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
