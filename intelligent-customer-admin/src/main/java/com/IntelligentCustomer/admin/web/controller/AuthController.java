package com.IntelligentCustomer.admin.web.controller;

import java.util.Map;

import com.IntelligentCustomer.system.domain.dto.auth.LoginRequest;
import com.IntelligentCustomer.system.domain.vo.auth.LoginResponse;
import com.IntelligentCustomer.system.domain.dto.auth.RegisterRequest;
import com.IntelligentCustomer.system.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "登录成功",
            "data", response
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "注册成功"
        ));
    }
}
