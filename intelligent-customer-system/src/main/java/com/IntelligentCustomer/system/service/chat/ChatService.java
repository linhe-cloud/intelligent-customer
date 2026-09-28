package com.IntelligentCustomer.system.service.chat;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.IntelligentCustomer.system.domain.vo.ChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.domain.dto.kafka.ChatMessageEvent;
import com.IntelligentCustomer.system.kafka.producer.ChatMessageProducer;
import com.IntelligentCustomer.system.service.search.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;
    private final ChatMessageProducer chatMessageProducer;
    private final SearchService searchService;

    private static final String HISTORY_KEY_PREFIX = "chat:history:";
    private static final int MAX_HISTORY_SIZE = 20;         // 最大保留信息数
    private static final long SESSION_EXPIRE_MINUTES = 60;  // 会话过期时间（分钟）

    public ChatService(ChatClient.Builder chatclientBuilder, StringRedisTemplate redisTemplate,
                       ChatMessageProducer chatMessageProducer, SearchService searchService) {
        this.chatClient = chatclientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.chatMessageProducer = chatMessageProducer;
        this.searchService = searchService;
    }

    public ChatResult chat(String sessionId, String message) {

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        // 读取历史
        String key = HISTORY_KEY_PREFIX + sessionId;
        List<String> history = redisTemplate.opsForList().range(key, 0, MAX_HISTORY_SIZE - 1);

        // RAG检索：从知识库中检索相关知识
        List<KnowledgeVector> knowledgeResults = searchService.hybridSearch(message, 5);
        logger.info("RAG检索到 {} 条知识库内容: sessionId={}", knowledgeResults.size(), sessionId);

        // 构建system prompt
        String systemPrompt;
        if (!knowledgeResults.isEmpty()) {
            StringBuilder knowledgeContext = new StringBuilder();
            for (int i = 0; i < knowledgeResults.size(); i++) {
                knowledgeContext.append(i + 1).append(". ").append(knowledgeResults.get(i).getContent()).append("\n");
            }
            systemPrompt = "你是一个智能客服助手。请根据以下知识库内容回答用户问题。如果知识库中没有相关信息，请如实告知用户。\n\n"
                    + "【知识库参考】\n" + knowledgeContext;
        } else {
            systemPrompt = "你是一个智能客服助手。请回答用户的问题。";
        }

        // 组装user prompt（历史 + 当前问题）
        StringBuilder userPrompt = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            for (String msg : history) {
                userPrompt.append(msg).append("\n");
            }
        }
        userPrompt.append(message).append("\n");

        // 调用ChatClient
        long startTime = System.currentTimeMillis();
        String reply = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt.toString())
                .call()
                .content();
        long responseTime = System.currentTimeMillis() - startTime;

        // 构建并发送对话事件到Kafka（发送失败不应影响正常返回）
        try {
            ChatMessageEvent event = new ChatMessageEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setSessionId(sessionId);
            event.setUserMessage(message);
            event.setAssistantReply(reply);
            event.setTimestamp(LocalDateTime.now());
            event.setResponseTime(responseTime);
            chatMessageProducer.send(event);
        } catch (Exception e) {
            logger.error("发送对话事件到Kafka失败: sessionId={}", sessionId, e);
        }

        // 保存会话历史
        redisTemplate.opsForList().rightPush(key, "用户：" + message);
        redisTemplate.opsForList().rightPush(key, "助手：" + reply);
        redisTemplate.opsForList().trim(key, -MAX_HISTORY_SIZE, -1);
        redisTemplate.expire(key, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES); // 会话过期时间

        ChatResult result = new ChatResult();
        result.setSessionId(sessionId);
        result.setReply(reply);
        return result;
    }

    // 生成安全的会话ID
    public String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] buffer = new byte[32];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
