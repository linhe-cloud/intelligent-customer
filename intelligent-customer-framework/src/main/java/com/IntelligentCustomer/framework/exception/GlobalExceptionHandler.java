package com.IntelligentCustomer.framework.exception;

import java.util.Map;

import com.IntelligentCustomer.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局处理异常

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 业务异常 -> 400
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException e) {
        String message = e.getMessage();
        if (e.getCause() != null) {
            message += "; 原始异常: " + e.getCause().getMessage();
        }
        return ResponseEntity.badRequest()
            .body(Map.of("code", 400, "message", message));
    }

    // 参数异常 -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleParam(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("code", 400, "message", e.getMessage()));
    }

    // 其他异常 -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        return ResponseEntity.status(500)
            .body(Map.of("code", 500, "message", "服务器内部错误"));
    }
}
