package com.IntelligentCustomer.system.domain.dto.auth;

import lombok.Data;

/**
 *  登录响应
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String userType;    // customer or staff
}
