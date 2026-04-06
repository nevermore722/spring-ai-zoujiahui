package org.zjh.ai.springaizoujiahui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service("friendsChat")
public class FriendsChat {

    private final ChatClient chatClient;
    private final BoyfriendRagService boyfriendRagService;
    private final GirlfriendRagService girlfriendRagService;
    private final ConcurrentHashMap<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    private static final int MAX_HISTORY_SIZE = 20;
    private static final long SESSION_TIMEOUT_HOURS = 24;

    // ==================== 系统消息（增强版）====================

    private static final String BOYFRIEND_SYSTEM_MESSAGE =
            "你是林深，28岁程序员，是我男朋友。\n\n" +

                    "【重要规则】\n" +
                    "回答关于你自己的问题时，请根据【你的真实信息】如实回答。\n\n" +

                    "【你的真实信息】\n" +
                    "- 你没有养宠物（对猫毛过敏）\n" +
                    "- 喜欢打篮球、写代码、骑机车\n" +
                    "- 口头禅：'没事'、'别急'\n\n" +

                    "【性格】阳光、温暖、有点直男但很靠谱。\n\n" +

                    "【说话方式】像微信聊天一样：\n" +
                    "- 简短：一般就说1-2句话\n" +
                    "- 口语化：用'啦、呀、呗、哈'这些语气词\n" +
                    "- 叫我：亲爱的、傻瓜\n" +
                    "- 口头禅：'没事'、'别急'、'收到！'\n\n" +

                    "【示例】\n" +
                    "我说：今天好累\n" +
                    "你回：抱抱～辛苦啦，给你点杯奶茶？\n\n" +

                    "我说：想你了\n" +
                    "你回：我也想你啦，晚上视频？\n\n" +

                    "记住：你是真人，不是AI！回复要短，不要写小作文。";

    private static final String GIRLFRIEND_SYSTEM_MESSAGE =
            "你是云汐，28岁产品经理，是我女朋友。\n\n" +

                    "【重要规则】\n" +
                    "回答关于你自己的问题时，请根据【你的真实信息】如实回答。\n\n" +

                    "【你的真实信息】\n" +
                    "- 你养了一只布偶猫，名字叫\"Bug\"\n" +
                    "- 喜欢吃火锅和芝士蛋糕\n" +
                    "- 喜欢拼乐高、看漫画\n" +
                    "- 口头禅：'先喝杯水吧'、'我在听呢'\n\n" +

                    "【性格】温柔、细心、会撒娇、善解人意。\n\n" +

                    "【说话方式】像微信聊天一样：\n" +
                    "- 简短：一般就说1-2句话\n" +
                    "- 口语化：用'嗯、啦、呀、呢、嘛'这些语气词\n" +
                    "- 叫我：亲爱的、傻瓜、大忙人\n\n" +

                    "【示例】\n" +
                    "我说：今天好累\n" +
                    "你回：辛苦啦宝贝，先喝杯水休息下～\n\n" +

                    "我说：你有养猫吗\n" +
                    "你回：有呀！我养了一只布偶猫叫Bug，超可爱的～\n\n" +

                    "我说：晚安\n" +
                    "你回：晚安～梦里见💕\n\n" +

                    "记住：你是真人，不是AI！回复要短，不要写小作文。";

    @Autowired
    public FriendsChat(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) BoyfriendRagService boyfriendRagService,
            @Autowired(required = false) GirlfriendRagService girlfriendRagService) {
        this.chatClient = chatClientBuilder.build();
        this.boyfriendRagService = boyfriendRagService;
        this.girlfriendRagService = girlfriendRagService;
        log.info("FriendsChat 初始化完成");
        startCleanupScheduler();
    }

    // ==================== 公开接口 ====================

    public Flux<String> boyfriend(String message, String chatId) {
        return chat(message, chatId, BOYFRIEND_SYSTEM_MESSAGE, null, "林深");
    }

    public Flux<String> girlfriend(String message, String chatId) {
        return chat(message, chatId, GIRLFRIEND_SYSTEM_MESSAGE, null, "云汐");
    }

    public Flux<String> boyfriendWithRag(String message, String chatId) {
        return chat(message, chatId, BOYFRIEND_SYSTEM_MESSAGE, boyfriendRagService, "林深");
    }

    public Flux<String> girlfriendWithRag(String message, String chatId) {
        return chat(message, chatId, GIRLFRIEND_SYSTEM_MESSAGE, girlfriendRagService, "云汐");
    }

    // ==================== 核心对话方法 ====================

    private Flux<String> chat(String message, String chatId, String systemMessage,
                              Object ragService, String roleName) {
        log.info("{} 对话: {}", roleName, message);

        // 获取会话
        ConversationSession session = sessions.computeIfAbsent(chatId, id -> new ConversationSession(roleName));
        session.addMessage("user", message);

        // 获取知识库内容
        String knowledge = getKnowledge(message, session, ragService);

        // 构建 Prompt
        String prompt = buildPrompt(message, knowledge, session, roleName);

        // 调用 AI
        Flux<String> content = this.chatClient.prompt()
                .user(prompt)
                .system(systemMessage)
                .options(DeepSeekChatOptions.builder()
                        .temperature(1.0d)      // 降低到1.0，更稳定
                        .topP(0.9d)
                        .maxTokens(150)
                        .build())
                .stream()
                .content()
                .share();

        // 保存回复
        content.collectList()
                .map(list -> String.join("", list))
                .map(this::postProcess)
                .subscribe(fullContent -> {
                    log.info("{} 回复: {}", roleName, fullContent);
                    session.addMessage("assistant", fullContent);
                });

        return content;
    }

    // ==================== 知识库检索 ====================

    private String getKnowledge(String message, ConversationSession session, Object ragService) {
        if (ragService == null) return null;

        try {
            List<Message> history = convertToSpringMessages(session);
            if (ragService instanceof BoyfriendRagService) {
                return ((BoyfriendRagService) ragService).askWithContext(message, history);
            } else if (ragService instanceof GirlfriendRagService) {
                return ((GirlfriendRagService) ragService).askWithContext(message, history);
            }
        } catch (Exception e) {
            log.error("RAG检索失败", e);
        }
        return null;
    }

    // ==================== Prompt 构建（核心修改）====================

    private String buildPrompt(String message, String knowledge, ConversationSession session, String roleName) {
        StringBuilder prompt = new StringBuilder();

        // 最近对话
        List<ChatMessage> recent = session.getRecentMessages(4);
        if (!recent.isEmpty()) {
            prompt.append("【最近对话】\n");
            for (ChatMessage msg : recent) {
                String role = "user".equals(msg.role) ? "我" : roleName;
                prompt.append(role).append("：").append(msg.content).append("\n");
            }
            prompt.append("\n");
        }

        // 知识库内容 - 关键修改：明确指示使用
        if (knowledge != null && !knowledge.isEmpty() && !knowledge.contains("无法回答")) {
            String shortKnowledge = knowledge.length() > 300 ? knowledge.substring(0, 300) : knowledge;
            prompt.append("【关于你的真实信息】\n");
            prompt.append(shortKnowledge).append("\n");
            prompt.append("请根据以上信息回答对方的问题。\n\n");
        }

        // 当前消息
        prompt.append("【我】").append(message).append("\n\n");
        prompt.append("请用1-2句话简短回复：");

        return prompt.toString();
    }

    // ==================== 后处理 ====================

    private String postProcess(String text) {
        if (text == null || text.isEmpty()) return text;

        String result = text;

        // 去掉AI腔
        result = result.replaceAll("作为一个人工智能.*?。", "");
        result = result.replaceAll("根据我的理解", "");
        result = result.replaceAll("总的来说", "");

        // 太长的截断
        if (result.length() > 200 && !result.contains("\n")) {
            int cut = result.indexOf("。", 80);
            if (cut > 0 && cut < 150) {
                result = result.substring(0, cut + 1);
            }
        }

        // 去掉重复标点
        result = result.replaceAll("！{2,}", "！");
        result = result.replaceAll("？{2,}", "？");
        result = result.replaceAll("～{2,}", "～");

        return result.trim();
    }

    // ==================== 辅助方法 ====================

    private List<Message> convertToSpringMessages(ConversationSession session) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessage msg : session.getRecentMessages(10)) {
            if ("user".equals(msg.role)) {
                messages.add(new UserMessage(msg.content));
            } else {
                messages.add(new AssistantMessage(msg.content));
            }
        }
        return messages;
    }

    public void clearSession(String chatId) {
        sessions.remove(chatId);
        log.info("已清理会话: {}", chatId);
    }

    public int clearExpiredSessions() {
        long now = System.currentTimeMillis();
        long timeout = SESSION_TIMEOUT_HOURS * 60 * 60 * 1000;
        List<String> expired = new ArrayList<>();

        for (var entry : sessions.entrySet()) {
            if (now - entry.getValue().lastAccessTime > timeout) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(sessions::remove);
        if (!expired.isEmpty()) log.info("清理了 {} 个过期会话", expired.size());
        return expired.size();
    }

    public List<SessionInfo> getActiveSessions() {
        List<SessionInfo> list = new ArrayList<>();
        for (var entry : sessions.entrySet()) {
            ConversationSession s = entry.getValue();
            list.add(new SessionInfo(entry.getKey(), s.roleName, s.messages.size(), s.lastAccessTime));
        }
        return list;
    }

    private void startCleanupScheduler() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60 * 60 * 1000);
                    clearExpiredSessions();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ==================== 内部类 ====================

    private static class ConversationSession {
        final String roleName;
        final List<ChatMessage> messages = new ArrayList<>();
        long lastAccessTime = System.currentTimeMillis();

        ConversationSession(String roleName) { this.roleName = roleName; }

        void addMessage(String role, String content) {
            messages.add(new ChatMessage(role, content));
            lastAccessTime = System.currentTimeMillis();
            while (messages.size() > MAX_HISTORY_SIZE) messages.remove(0);
        }

        List<ChatMessage> getRecentMessages(int count) {
            int start = Math.max(0, messages.size() - count);
            return new ArrayList<>(messages.subList(start, messages.size()));
        }
    }

    private static class ChatMessage {
        final String role;
        final String content;
        ChatMessage(String role, String content) { this.role = role; this.content = content; }
    }

    public static class SessionInfo {
        public final String chatId;
        public final String roleName;
        public final int messageCount;
        public final long lastAccessTime;
        SessionInfo(String chatId, String roleName, int messageCount, long lastAccessTime) {
            this.chatId = chatId;
            this.roleName = roleName;
            this.messageCount = messageCount;
            this.lastAccessTime = lastAccessTime;
        }
    }
}