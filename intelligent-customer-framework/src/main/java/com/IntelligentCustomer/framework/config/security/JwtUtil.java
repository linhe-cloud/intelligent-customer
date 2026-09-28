package com.IntelligentCustomer.framework.config.security;


import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

/**
 *  JWT 工具类
 */
@Configuration
public class JwtUtil {
    
    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:mySecretKeyForJwtTokenGenerationAtLeast32Chars}") String secretKey,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.expiration = expiration;
    }

    // 生成 Token
    public String generateToken(String id, String usertype, String role) {
        return Jwts.builder()
                .subject(id)
                .claim("usertype", usertype)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    // 解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 获取用户Id
    public String getId(String token) {
        return parseToken(token).getSubject();
    }

    // 获取用户类型
    public String getUserType(String token) {
        return parseToken(token).get("usertype", String.class);
    }

    // 获取用户角色
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    // 判断 Token 是否过期
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    // 判断 Token 是否过期（别名）
    public boolean isExpired(String token) {
        return isTokenExpired(token);
    }

    // 获取 Token 过期时间
    public long getExpiration(String token) {
        return parseToken(token).getExpiration().getTime();
    }
}
