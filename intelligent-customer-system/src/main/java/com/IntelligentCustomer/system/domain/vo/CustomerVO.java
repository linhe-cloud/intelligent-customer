package com.IntelligentCustomer.system.domain.vo;

import com.IntelligentCustomer.system.domain.entity.Customer;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * 客户对外响应对象。
 *
 * <p>不包含 passwordHash，避免数据库敏感字段返回给客户端。</p>
 */
@Data
public class CustomerVO {

    private UUID id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private String status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerVO from(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerVO vo = new CustomerVO();
        vo.setId(customer.getId());
        vo.setUsername(customer.getUsername());
        vo.setName(customer.getName());
        vo.setEmail(customer.getEmail());
        vo.setPhone(customer.getPhone());
        vo.setAvatar(customer.getAvatar());
        vo.setStatus(customer.getStatus());
        vo.setLastLogin(customer.getLastLogin());
        vo.setCreatedAt(customer.getCreatedAt());
        vo.setUpdatedAt(customer.getUpdatedAt());
        return vo;
    }
}
