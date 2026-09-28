package com.IntelligentCustomer.system.domain.dto.auth;

import lombok.Data;

/**
 * 注册请求体
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String userType;    // customer or staff
    private String role;        // staff专用: ADMIN, AGENT等
}
