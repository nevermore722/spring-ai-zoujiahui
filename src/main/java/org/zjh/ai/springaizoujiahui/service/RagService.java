package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        log.info("RAG 服务初始化完成");
    }

    /**
     * 基于知识库的问答
     */
    public String ask(String question) {
        log.info("RAG 问答: {}", question);

        // 1. 向量检索相关文档
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(3)                      // 返回最相关的3个文档
                .similarityThreshold(0.5)     // 相似度阈值
                .build();

        var relevantDocs = vectorStore.similaritySearch(searchRequest);

        // 2. 构建上下文
        String context = relevantDocs.stream()
                .map(doc -> doc.getText())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        log.info("检索到 {} 个相关文档", relevantDocs.size());

        // 3. 构建 Prompt
        String prompt = buildPrompt(question, context);

        // 4. 调用 LLM
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("RAG 回答: {}", answer);
        return answer;
    }

    private String buildPrompt(String question, String context) {
        if (context == null || context.isEmpty()) {
            return question;
        }

        return """
                请基于以下参考信息回答问题。如果参考信息中没有相关答案，请说"根据现有资料无法回答"。
                
                参考信息：
                %s
                
                问题：%s
                
                回答：
                """.formatted(context, question);
    }
}