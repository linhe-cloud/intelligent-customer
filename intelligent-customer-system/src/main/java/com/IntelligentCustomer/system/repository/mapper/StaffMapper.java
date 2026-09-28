package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.Staff;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StaffMapper extends BaseMapper<Staff> {

    default Staff findById(UUID id) {
        return selectById(id);
    }

    default List<Staff> findAll() {
        return selectList(new LambdaQueryWrapper<Staff>()
                .orderByDesc(Staff::getCreatedAt));
    }

    default List<Staff> findByRole(String role) {
        return selectList(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getRole, role)
                .orderByDesc(Staff::getCreatedAt));
    }

    default void update(Staff staff) {
        staff.setUpdatedAt(LocalDateTime.now());
        updateById(staff);
    }

    default Staff findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getUsername, username)
                .last("LIMIT 1"));
    }

    default void updateLastLogin(UUID id) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setLastLogin(LocalDateTime.now());
        updateById(staff);
    }
}
