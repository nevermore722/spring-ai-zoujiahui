package org.zjh.ai.springaizoujiahui.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatModelConfig {

    @Bean
    @Primary  // 这个注解是关键，告诉 Spring 优先使用这个 Bean
    public ChatModel primaryChatModel(DeepSeekChatModel deepSeekChatModel) {
        // 直接返回 Spring 自动配置好的 DeepSeekChatModel
        return deepSeekChatModel;
    }
}