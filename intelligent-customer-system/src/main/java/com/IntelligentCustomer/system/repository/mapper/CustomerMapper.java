package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.Customer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

/**
 * CustomerMapper接口，用于处理客户相关的数据库操作
 * 继承BaseMapper以获得基本的CRUD操作能力
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 根据ID查找客户
     * @param id 客户的唯一标识符
     * @return 返回找到的客户对象，如果未找到则返回null
     */
    default Customer findById(UUID id) {
        return selectById(id);
    }

    /**
     * 查找所有客户，并按创建时间降序排列
     * @return 返回客户列表，按创建时间从新到旧排序
     */
    default List<Customer> findAll() {
        return selectList(new LambdaQueryWrapper<Customer>()
                .orderByDesc(Customer::getCreatedAt));
    }

    /**
     * 更新客户信息
     * @param customer 包含更新后信息的客户对象
     */
    default void update(Customer customer) {
        customer.setUpdatedAt(LocalDateTime.now()); // 设置更新时间为当前时间
        updateById(customer);
    }

    /**
     * 根据用户名查找客户
     * @param username 客户的用户名
     * @return 返回找到的客户对象，如果未找到则返回null
     */
    default Customer findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUsername, username) // 设置查询条件为用户名匹配
                .last("LIMIT 1")); // 限制只返回一条结果
    }

    /**
     * 更新客户最后登录时间
     * @param id 客户的唯一标识符
     */
    default void updateLastLogin(UUID id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setLastLogin(LocalDateTime.now()); // 设置最后登录时间为当前时间
        updateById(customer);
    }
}
