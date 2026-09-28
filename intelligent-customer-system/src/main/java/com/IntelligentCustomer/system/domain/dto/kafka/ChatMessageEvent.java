package com.IntelligentCustomer.system.domain.dto.kafka;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessageEvent {
    private String eventId;             // 事件唯一ID（UUID）   
    private String sessionId;           // 会话ID
    private String userMessage;         // 用户消息
    private String assistantReply;      // 模型回复
    private LocalDateTime timestamp;    // 时间戳
    private long responseTime;          // 响应时间（毫秒）
}
