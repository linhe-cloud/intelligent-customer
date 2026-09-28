package com.IntelligentCustomer.system.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.entity.ChatHistory;
import com.IntelligentCustomer.system.domain.dto.kafka.ChatMessageEvent;
import com.IntelligentCustomer.system.repository.mapper.ChatHistoryMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        try {
            // 序列化
            ChatMessageEvent event = objectMapper.readValue(message, ChatMessageEvent.class);

            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setId(UUID.randomUUID());
            chatHistory.setEventId(event.getEventId());
            chatHistory.setSessionId(event.getSessionId());
            chatHistory.setUserMessage(event.getUserMessage());
            chatHistory.setAssistantReply(event.getAssistantReply());
            chatHistory.setTimestamp(event.getTimestamp());
            chatHistory.setResponseTime(event.getResponseTime());
            chatHistoryMapper.insert(chatHistory);
        } catch (Exception e) {
            log.error("对话记录持久化失效: {}", e.getMessage(), e);
        }
    }
}
