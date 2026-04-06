package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GirlfriendRagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public GirlfriendRagService(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("girlfriendVectorStore") VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        log.info("女朋友 RAG 服务初始化完成（生活知识库）");
    }

    public String askWithContext(String question, List<Message> history) {
        log.info("女朋友RAG查询: {}", question);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        var relevantDocs = vectorStore.similaritySearch(searchRequest);
        log.info("检索到 {} 个相关文档", relevantDocs.size());

        String context = relevantDocs.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.joining("\n\n"));

        if (context.isEmpty()) {
            return null;
        }

        return context;
    }
}