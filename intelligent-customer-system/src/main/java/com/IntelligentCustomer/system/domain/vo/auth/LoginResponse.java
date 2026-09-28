package com.IntelligentCustomer.system.domain.vo.auth;

import lombok.Data;

/**
 *  登录响应
 */
@Data
public class LoginResponse {
    private String token;
    private String usertype;
    private String id;
    private String role;    // Staff 角色
    private long expiresIn; // Token 过期时间

}
