package com.IntelligentCustomer.system.domain.vo;

import lombok.Data;

/**
 *  对话响应
 */
@Data
public class ChatResponse {
    private String sessionId;
    private String reply;
}
