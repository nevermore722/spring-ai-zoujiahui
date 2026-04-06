package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, @Qualifier("boyfriendVectorStore")VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        log.info("RAG 服务初始化完成");
    }

    /**
     * 基础 RAG 问答
     */
    public String ask(String question) {
        return askWithContext(question, null);
    }

    /**
     * 带对话历史的 RAG 问答
     * @param question 当前问题
     * @param conversationHistory 历史对话（可选）
     */
    public String askWithContext(String question, List<Message> conversationHistory) {
        log.info("RAG 问答: {}", question);

        // 1. 构建检索查询（结合历史上下文）
        String searchQuery = buildSearchQuery(question, conversationHistory);

        // 2. 向量检索相关文档
        SearchRequest searchRequest = SearchRequest.builder()
                .query(searchQuery)
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        var relevantDocs = vectorStore.similaritySearch(searchRequest);
        log.info("检索到 {} 个相关文档", relevantDocs.size());

        // 3. 构建上下文
        String context = relevantDocs.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.joining("\n\n"));

        // 4. 构建带历史和角色的 Prompt
        String prompt = buildNaturalPrompt(question, context, conversationHistory);

        // 5. 调用 LLM
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("RAG 回答: {}", answer);
        return answer;
    }

    /**
     * 构建检索查询（结合历史上下文提取关键信息）
     */
    private String buildSearchQuery(String question, List<Message> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }

        // 从历史中提取最近几条消息的关键信息
        StringBuilder contextQuery = new StringBuilder(question);
        int recentCount = Math.min(3, history.size());
        for (int i = history.size() - recentCount; i < history.size(); i++) {
            Message msg = history.get(i);
            if (msg instanceof UserMessage) {
                contextQuery.append(" ").append(msg.getText());
            }
        }
        return contextQuery.toString();
    }

    /**
     * 构建自然的 Prompt（更像真人对话）
     */
    private String buildNaturalPrompt(String question, String context, List<Message> history) {
        StringBuilder prompt = new StringBuilder();

        // 1. 系统指令
        prompt.append("你是一个真实的人，名叫云汐。请用自然、亲切、口语化的方式回答问题。\n");
        prompt.append("规则：\n");
        prompt.append("1. 不要像机器人一样照搬知识库内容，要用自己的话自然地说出来\n");
        prompt.append("2. 如果有知识库信息，自然地融入回答，不要说'根据资料显示'之类的机械表达\n");
        prompt.append("3. 保持女朋友的语气：温柔、关心、偶尔撒娇\n");
        prompt.append("4. 回答要简洁，不要太长，就像微信聊天一样\n");
        prompt.append("5. 适当加入语气词（嗯、啦、呀、呢）和表情符号 😊\n\n");

        // 2. 对话历史（让 AI 记住上下文）
        if (history != null && !history.isEmpty()) {
            prompt.append("【最近的对话记录】\n");
            int recentCount = Math.min(5, history.size());
            List<Message> recentHistory = history.subList(history.size() - recentCount, history.size());
            for (Message msg : recentHistory) {
                if (msg instanceof UserMessage) {
                    prompt.append("我：").append(msg.getText()).append("\n");
                } else if (msg instanceof AssistantMessage) {
                    prompt.append("云汐：").append(msg.getText()).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 3. 知识库参考（如果存在）
        if (context != null && !context.isEmpty()) {
            prompt.append("【可以参考的信息】\n");
            prompt.append(context).append("\n");
            prompt.append("（请用你自己的话自然地说出这些信息，不要直接复制）\n\n");
        }

        // 4. 当前问题
        prompt.append("【现在】\n");
        prompt.append("我对你说：").append(question).append("\n\n");
        prompt.append("请用云汐的身份自然地回复我：");

        return prompt.toString();
    }
}