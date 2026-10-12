package com.IntelligentCustomer.system.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.entity.ChatHistory;
import com.IntelligentCustomer.system.domain.dto.kafka.ChatMessageEvent;
import com.IntelligentCustomer.system.repository.mapper.ChatHistoryMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DuplicateKeyException;
import java.util.UUID;

@Service
public class ChatHistoryConsumer {
    private static final Logger log = LoggerFactory.getLogger(ChatHistoryConsumer.class);

    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    public ChatHistoryConsumer(ChatHistoryMapper chatHistoryMapper, ObjectMapper objectMapper) {
        this.chatHistoryMapper = chatHistoryMapper;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "chat-message", groupId = "chat-history-consumer-group")
    public void consume(String message) {
        ChatMessageEvent event = parseMessage(message);

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("聊天事件缺少 eventId");
        }

        if (chatHistoryMapper.findByEventId(event.getEventId()) != null) {
            log.info("聊天事件已处理，跳过重复消息: eventId={}", event.getEventId());
            return;
        }

        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setId(UUID.randomUUID());
        chatHistory.setEventId(event.getEventId());
        chatHistory.setSessionId(event.getSessionId());
        chatHistory.setUserMessage(event.getUserMessage());
        chatHistory.setAssistantReply(event.getAssistantReply());
        chatHistory.setTimestamp(event.getTimestamp());
        chatHistory.setResponseTime(event.getResponseTime());

        try {
            chatHistoryMapper.insert(chatHistory);
        } catch (DuplicateKeyException exception) {
            log.info("聊天事件重复写入，视为处理成功: eventId={}", event.getEventId());
        } catch (RuntimeException exception) {
            log.error("对话记录持久化失败: eventId={}", event.getEventId(), exception);
            throw exception;
        }
    }

    private ChatMessageEvent parseMessage(String message) {
        try {
            return objectMapper.readValue(message, ChatMessageEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("聊天消息格式不正确", exception);
        }
    }
}
