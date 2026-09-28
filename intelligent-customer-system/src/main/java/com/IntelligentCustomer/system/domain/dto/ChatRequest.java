package com.IntelligentCustomer.system.domain.dto;

import lombok.Data;


@Data
public class ChatRequest {
    private String sessionId;   // 会话ID
    private String message;
}
