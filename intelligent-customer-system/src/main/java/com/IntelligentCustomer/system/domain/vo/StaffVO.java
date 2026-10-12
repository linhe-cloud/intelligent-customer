package com.IntelligentCustomer.system.domain.vo;

import com.IntelligentCustomer.system.domain.entity.Staff;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * 客服对外响应对象。
 *
 * <p>不包含 passwordHash，避免数据库敏感字段返回给客户端。</p>
 */
@Data
public class StaffVO {

    private UUID id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String department;
    private String status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffVO from(Staff staff) {
        if (staff == null) {
            return null;
        }

        StaffVO vo = new StaffVO();
        vo.setId(staff.getId());
        vo.setUsername(staff.getUsername());
        vo.setName(staff.getName());
        vo.setEmail(staff.getEmail());
        vo.setPhone(staff.getPhone());
        vo.setRole(staff.getRole());
        vo.setDepartment(staff.getDepartment());
        vo.setStatus(staff.getStatus());
        vo.setLastLogin(staff.getLastLogin());
        vo.setCreatedAt(staff.getCreatedAt());
        vo.setUpdatedAt(staff.getUpdatedAt());
        return vo;
    }
}
