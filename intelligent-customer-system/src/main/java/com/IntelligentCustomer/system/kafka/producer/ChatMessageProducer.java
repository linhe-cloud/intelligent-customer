package com.IntelligentCustomer.system.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.dto.kafka.ChatMessageEvent;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageProducer.class);
    private static final String TOPIC = "chat-message";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    public ChatMessageProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(ChatMessageEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(TOPIC, event.getSessionId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            logger.error("对话事件发送失败: sessionId={}, error={}", event.getSessionId(), ex.getMessage());
                        } else {
                            logger.info("对话事件发送成功: sessionId={}, partition={}, offset={}", event.getSessionId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            // 因为是对话事件，所以这里不抛异常，只记录日志
            logger.error("对话事件发送失败: sessionId={}", event.getSessionId(), e);
        }
    }
}
