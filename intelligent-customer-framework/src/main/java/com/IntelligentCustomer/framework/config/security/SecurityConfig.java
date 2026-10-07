package com.IntelligentCustomer.framework.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 *  安全配置类
 *  用于配置Spring Security的安全策略，包括JWT认证、跨域配置、密码加密等
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // JWT过滤器，用于处理JWT认证
    private final JwtFilter jwtFilter;

    /**
     * 构造函数，注入JWT过滤器
     * @param jwtFilter JWT过滤器
     */
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * 配置安全过滤器链
     * @param http HttpSecurity对象，用于配置安全策略
     * @return 配置好的SecurityFilterChain
     * @throws Exception 可能抛出的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护，因为使用JWT认证
            .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF保护，JWT是无状态的，不需要CSRF保护
            // 配置CORS跨域策略
            .cors(Customizer.withDefaults())  // 启用CORS并使用默认配置
            // 设置为无状态会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 使用无状态会话，适用于JWT认证
            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口，无需认证
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()

                // Staff接口，需要STAFF角色
                .requestMatchers("/document/**").hasRole("STAFF")  // 访问/document/下的资源需要STAFF角色
                .requestMatchers("/embedding/**").hasRole("STAFF")  // 访问/embedding/下的资源需要STAFF角色
                .requestMatchers("/staff/**").hasRole("ADMIN")  // 员工管理接口仅管理员可访问
                // DELETE请求的/customer/**和/staff/**接口，需要ADMIN角色
                .requestMatchers(HttpMethod.DELETE, "/customer/**").hasRole("ADMIN")  // 删除客户信息需要管理员权限

                // 其他需要认证
                .anyRequest().authenticated()  // 除上述配置外的所有请求都需要认证
            )
                // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {  // 未认证时的处理
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 设置HTTP状态码为401
                    response.setContentType("application/json;charset=UTF-8");  // 设置响应内容类型为JSON
                    response.getWriter().write(  // 返回错误信息
                        "{\"code\":401,\"message\":\"未登录或登录已过期\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {  // 权限不足时的处理
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 设置HTTP状态码为403
                    response.setContentType("application/json;charset=UTF-8");  // 设置响应内容类型为JSON
                    response.getWriter().write(  // 返回错误信息
                        "{\"code\":403,\"message\":\"没有权限访问该资源\"}"
                    );
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);  // 添加JWT过滤器，在用户名密码认证过滤器之前执行
        return http.build();  // 构建并返回安全过滤器链
    }

/**
 * 配置跨域资源共享(CORS)的Bean方法
 * 该方法创建并配置一个CorsConfigurationSource Bean，用于处理跨域请求
 *
 * @return 配置好的CorsConfigurationSource实例，用于全局跨域设置
 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
    // 创建一个新的CorsConfiguration对象，用于配置跨域规则
        CorsConfiguration configuration = new CorsConfiguration();
    // 设置允许的前端源模式，这里允许所有以http://localhost:开头的源
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
    // 设置允许的HTTP方法，包括GET、POST、PUT、DELETE和OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    // 设置允许的请求头，"*"表示允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
    // 设置是否允许发送凭据信息，如Cookie、HTTP认证等
        configuration.setAllowCredentials(true);
    // 设置预检请求的有效期，单位为秒，3600表示1小时内无需再次预检
        configuration.setMaxAge(3600L);

    // 创建基于URL的CorsConfigurationSource，用于应用跨域配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 注册跨域配置，"/**"表示对所有路径应用此跨域配置
        source.registerCorsConfiguration("/**", configuration);
    // 返回配置完成的CorsConfigurationSource
        return source;
    }

/**
 * 配置密码编码器Bean
 * 使用BCryptPasswordEncoder进行密码加密
 * BCrypt是一种安全的哈希算法，特别适合用于密码存储
 *
 * @return 返回一个BCryptPasswordEncoder实例
 *         用于Spring Security进行密码加密和验证
 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
