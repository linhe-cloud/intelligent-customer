package com.IntelligentCustomer.system.domain.dto;

import lombok.Data;


/**
 * ChatRequest类用于封装聊天请求的数据结构
 * 使用@Data注解自动生成getter、setter、toString等方法
 */
@Data
public class ChatRequest {
    private String sessionId;   // 会话ID，用于标识唯一对话
    private String message;     // 聊天消息内容
}
