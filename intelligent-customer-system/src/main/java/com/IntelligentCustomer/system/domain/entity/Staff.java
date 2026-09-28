package com.IntelligentCustomer.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
@TableName("staff")
public class Staff {
    @TableId("id")
    private UUID id;
    private String username;
    @TableField("password_hash")
    private String passwordHash;    
    private String name;
    private String email;
    private String phone;
    private String role;
    private String department;
    private String status;
    @TableField("last_login")
    private LocalDateTime lastLogin;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
