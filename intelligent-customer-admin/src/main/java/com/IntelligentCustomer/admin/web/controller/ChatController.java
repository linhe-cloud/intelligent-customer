package com.IntelligentCustomer.admin.web.controller;

import java.security.Principal;

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

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        logger.info("用户对话: userId={}, sessionId={}", userId, request.getSessionId());
        ChatResult result = chatService.chat(request.getSessionId(), request.getMessage());
        ChatResponse response = new ChatResponse();
        response.setSessionId(result.getSessionId());
        response.setReply(result.getReply());
        return response;
    }
}
