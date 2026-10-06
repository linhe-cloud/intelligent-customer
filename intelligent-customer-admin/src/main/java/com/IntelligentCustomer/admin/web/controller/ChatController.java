package com.IntelligentCustomer.admin.web.controller;

import java.security.Principal;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.vo.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.dto.ChatRequest;
import com.IntelligentCustomer.system.domain.vo.ChatResult;
import com.IntelligentCustomer.system.service.chat.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    /**
     * 构造函数，通过依赖注入方式初始化ChatService
     * @param chatService 聊天服务接口，负责处理具体的聊天业务逻辑
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

/**
 * 处理用户对话请求的POST接口方法
 * @param request 包含用户会话ID和消息内容的请求对象
 * @param principal 当前认证用户信息
 * @return ChatResponse 包含回复内容和会话ID的响应对象
 */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request, Principal principal) {

        if (principal == null) { throw new BusinessException("用户未登录"); }

    // 获取用户ID
        String userId = principal.getName();
    // 记录用户对话日志，包含用户ID和会话ID
        logger.info("用户对话: userId={}, sessionId={}", userId, request.getSessionId());
    // 调用聊天服务处理对话请求
        ChatResult result = chatService.chat(userId, request.getSessionId(), request.getMessage());
    // 创建并设置响应对象
        ChatResponse response = new ChatResponse();
        response.setSessionId(result.getSessionId());
        response.setReply(result.getReply());
    // 返回响应对象
        return response;
    }
}
