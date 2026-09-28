package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.Customer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    default Customer findById(UUID id) {
        return selectById(id);
    }

    default List<Customer> findAll() {
        return selectList(new LambdaQueryWrapper<Customer>()
                .orderByDesc(Customer::getCreatedAt));
    }

    default void update(Customer customer) {
        customer.setUpdatedAt(LocalDateTime.now());
        updateById(customer);
    }

    default Customer findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUsername, username)
                .last("LIMIT 1"));
    }

    default void updateLastLogin(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setLastLogin(LocalDateTime.now());
        updateById(customer);
    }
}
