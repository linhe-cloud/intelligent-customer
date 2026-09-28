package com.IntelligentCustomer.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@TableName("chat_history")
public class ChatHistory {
    @TableId("id")
    private UUID id;
    @TableField("event_id")
    private String eventId;
    @TableField("session_id")
    private String sessionId;
    @TableField("user_message")
    private String userMessage;
    @TableField("assistant_reply")
    private String assistantReply;
    private LocalDateTime timestamp;
    @TableField("response_time")
    private Long responseTime;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
