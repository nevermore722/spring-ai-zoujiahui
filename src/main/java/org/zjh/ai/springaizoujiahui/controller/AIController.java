package org.zjh.ai.springaizoujiahui.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("ai")
@Slf4j
public class AIController {

    // 智能对话的客户端
    private final ChatClient chatClient;

    public final DeepSeekChatModel chatModel;

    private static final String SYSTEM_MESSAGE = "你是一个算命专家，你需要一步一步引导用户问你问题进行算命，先问对方叫什么名字，提供几个算命方向并表明自己什么都能算，一次只问一个问题，但可以多问几步，最终返回算命结果";

    public AIController(ChatClient.Builder chatClientBuilder, DeepSeekChatModel chatModel) {
        chatClientBuilder.defaultSystem(SYSTEM_MESSAGE);
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(50)
                .build();
        this.chatClient = chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String generation(@RequestParam(value = "message", defaultValue = "我要算命") String message) {
        // 提示词
        return this.chatClient.prompt()
                // 用户信息
                .user(message)
                // 远程请求大模型
                .call()
                // 返回文本
                .content();
    }

    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam(value = "message", defaultValue = "我要算命") String message,
                               HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        // 提示词
        return this.chatClient.prompt()
                // 用户信息
                .user(message)
                // 远程请求大模型
                .stream()
                // 返回文本
                .content();
    }

    @GetMapping("/chat/model")
    public String chatModel(@RequestParam(value = "message", defaultValue = "我要算命") String message) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        SYSTEM_MESSAGE,
                        DeepSeekChatOptions.builder()
                                .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                                .temperature(0.8d)
                                .build()
                ));
        return response.getResult().getOutput().getText();
    }

    @GetMapping(value = "/stream/history", produces = "text/html;charset=UTF-8")
    public Flux<String> streamHistory(@RequestParam(value = "message", defaultValue = "我要算命") String message, @RequestParam String chatId) {
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