package com.IntelligentCustomer.system.service.chat;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.dto.search.HybridSearchResult;
import com.IntelligentCustomer.system.domain.vo.ChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.IntelligentCustomer.system.domain.dto.kafka.ChatMessageEvent;
import com.IntelligentCustomer.system.kafka.producer.ChatMessageProducer;
import com.IntelligentCustomer.system.service.search.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


/**
 * 聊天服务类，处理用户与AI助手的对话交互
 * 包含历史记录管理、RAG检索、会话ID生成等功能
 */
@Service
public class ChatService {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    // 依赖注入的组件
    private final ChatClient chatClient;                    // 聊天客户端，用于与AI模型交互
    private final StringRedisTemplate redisTemplate;        // Redis模板，用于操作缓存数据
    private final ChatMessageProducer chatMessageProducer;  // 消息生产者，用于发送消息到Kafka
    private final SearchService searchService;              // 搜索服务，用于RAG检索



    // Redis相关常量
    private static final String HISTORY_KEY_PREFIX = "chat:history:";  // 聊天历史记录的键前缀
    private static final long SESSION_EXPIRE_MINUTES = 60;  // 会话过期时间（分钟）
    private static final int MAX_MESSAGE_CHARS = 2000;      // 最大消息长度（字符数）
    private static final int MAX_HISTORY_TURNS = 6;        // 最大历史对话轮数
    private static final int MAX_HISTORY_MESSAGES = MAX_HISTORY_TURNS * 2;  // 最大历史对话消息数
    private static final int MAX_HISTORY_CHARS = 6000;      // 最大历史对话字符数
    private static final int MAX_KNOWLEDGE_CHARS = 5000;    // 最大知识库内容字符数
    private static final String NO_KNOWLEDGE_REPLY = "抱歉，我无法回答这个问题，建议联系人工客服进一步处理。";  // 无相关知识时的回复
    private static final int MAX_RETRIEVAL_QUERY_CHARS = 2000; // 最大检索查询字符数


    /**
     * 构造函数，注入所需的服务组件
     * @param chatclientBuilder 聊天客户端构建器
     * @param redisTemplate Redis模板
     * @param chatMessageProducer 聊天消息生产者
     * @param searchService 搜索服务
     */
    public ChatService(ChatClient.Builder chatclientBuilder, StringRedisTemplate redisTemplate,
                       ChatMessageProducer chatMessageProducer, SearchService searchService) {
        this.chatClient = chatclientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.chatMessageProducer = chatMessageProducer;
        this.searchService = searchService;
    }

    /**
     * 处理用户聊天请求
     * @param sessionId 会话ID，如果为空则生成新的
     * @param message 用户发送的消息
     * @return ChatResult 包含会话ID和AI助手的回复
     */
    public ChatResult chat(String userId, String sessionId, String message) {

        String normalizedMessage = normalizeMessage(message);

        if (userId == null || userId.isBlank()) {
            throw new BusinessException("用户身份不能为空");
        }

        // 如果会话ID为空，则生成新的会话ID
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = generateSessionId();
        }

        // 读取历史
        String key = HISTORY_KEY_PREFIX + userId + ":" + sessionId;
        List<String> storedHistory = redisTemplate.opsForList()
                .range(key, -MAX_HISTORY_MESSAGES, -1);

        List<String> history = limitHistory(storedHistory);

        // RAG检索：从知识库中检索相关知识
        String retrievalQuery = buildRetrievalQuery(
                history,
                normalizedMessage
        );

        List<HybridSearchResult> knowledgeResults =
                searchService.hybridSearch(retrievalQuery, 5);
        if (knowledgeResults == null) {
            knowledgeResults = List.of();
        }
        logger.info(
                "RAG检索到 {} 条知识库内容: sessionId={}",
                knowledgeResults.size(),
                sessionId
        );

        List<Message> historyMessages = toChatMessages(history);

        // 没有知识库依据时直接返回固定回复，避免模型自由编造答案。
        long startTime = System.currentTimeMillis();
        String reply;

        if (knowledgeResults.isEmpty()) {
            reply = NO_KNOWLEDGE_REPLY;
        } else {
            String knowledgeContext = buildKnowledgeContext(knowledgeResults);

            if (knowledgeContext.isBlank()) {
                reply = NO_KNOWLEDGE_REPLY;
            } else {
                String systemPrompt = """
                        你是一个严格基于知识库工作的智能客服。

                        回答规则：
                        1. 只能根据知识库内容回答。
                        2. 历史对话只用于理解上下文，不作为事实依据。
                        3. 不得根据常识、猜测或训练知识补充答案。
                        4. 知识库没有明确说明的内容，不得自行推测。
                        5. 回答要简洁、准确、清晰。

                        【知识库内容】
                        """ + knowledgeContext;

                reply = chatClient.prompt()
                        .system(systemPrompt)
                        .messages(historyMessages)
                        .user(normalizedMessage)
                        .call()
                        .content();
            }
        }
        long responseTime = System.currentTimeMillis() - startTime;
        String normalizedReply = reply == null ? "" : reply.strip();

        // 构建并发送对话事件到Kafka（发送失败不应影响正常返回）
        try {
            ChatMessageEvent event = new ChatMessageEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setSessionId(sessionId);
            event.setUserMessage(normalizedMessage);
            event.setAssistantReply(normalizedReply);
            event.setTimestamp(LocalDateTime.now());
            event.setResponseTime(responseTime);
            chatMessageProducer.send(event);
        } catch (Exception e) {
            logger.error("发送对话事件到Kafka失败: sessionId={}", sessionId, e);
        }

        // 保存会话历史
        redisTemplate.opsForList()
                .rightPush(key, "用户：" + normalizedMessage);
        redisTemplate.opsForList()
                .rightPush(key, "助手：" + normalizedReply);
        redisTemplate.opsForList()
                .trim(key, -MAX_HISTORY_MESSAGES, -1);
        redisTemplate.expire(key, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES); // 会话过期时间

        ChatResult result = new ChatResult();
        result.setSessionId(sessionId);
        result.setReply(normalizedReply);
        return result;
    }

    private List<String> limitHistory(List<String> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        List<String> selected = new ArrayList<>();
        int totalChars = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            String item = history.get(i);

            if (item == null || item.isBlank()) { continue; }

            String normalizedItem = item.strip();

            if (totalChars + normalizedItem.length() > MAX_HISTORY_CHARS) { break; }

            selected.add(normalizedItem);
            totalChars += normalizedItem.length();
        }

        Collections.reverse(selected);
        return selected;
    }

    private List<Message> toChatMessages(List<String> history) {
        List<Message> messages = new ArrayList<>();

        if (history == null || history.isEmpty()) {
            return messages;
        }

        for (String item : history) {
            if (item.startsWith("用户：")) {
                String content = item.substring(3).strip();

                if (!content.isBlank()) {
                    messages.add(new UserMessage(content));
                }
            } else if (item.startsWith("助手：")) {
                String content = item.substring(3).strip();

                if (!content.isBlank()) {
                    messages.add(new AssistantMessage(content));
                }
            }
        }

        return messages;
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("消息不能为空");
        }

        String normalizedMessage = message.strip();

        if (normalizedMessage.length() > MAX_MESSAGE_CHARS) {
            throw new BusinessException(
                    "消息长度不能超过 " + MAX_MESSAGE_CHARS + " 个字符"
            );
        }

        return normalizedMessage;
    }

    private String buildRetrievalQuery(
            List<String> history,
            String currentMessage
    ) {
        StringBuilder query = new StringBuilder();

        if (history != null) {
            for (String item : history) {
                if (item.startsWith("用户：")) {
                    query.append(item.substring(3)).append(" ");
                }
            }
        }

        query.append(currentMessage);

        String result = query.toString().strip();

        if (result.length() > MAX_RETRIEVAL_QUERY_CHARS) {
            return result.substring(
                    result.length() - MAX_RETRIEVAL_QUERY_CHARS
            );
        }

        return result;
    }

    private String buildKnowledgeContext(
            List<HybridSearchResult> results
    ) {
        StringBuilder context = new StringBuilder();
        Set<String> existingContents = new HashSet<>();

        for (HybridSearchResult result : results) {
            if (result == null
                    || result.getContent() == null
                    || result.getContent().isBlank()) {
                continue;
            }

            String content = result.getContent().strip();

            if (!existingContents.add(content)) {
                continue;
            }

            String separator = context.length() == 0 ? "" : "\n\n";
            if (context.length()
                    + separator.length()
                    + content.length()
                    > MAX_KNOWLEDGE_CHARS) {
                break;
            }

            context.append(separator).append(content);
        }

        return context.toString().strip();
    }

    // 生成安全的会话ID
    public String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] buffer = new byte[32];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
