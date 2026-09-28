package com.IntelligentCustomer.admin.web.controller;

import java.security.Principal;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.entity.Customer;
import com.IntelligentCustomer.system.repository.mapper.CustomerMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerMapper customerMapper;

    public CustomerController(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }


    // 新增客户
    @PostMapping("/add")
    public ResponseEntity<?> create(@RequestBody Customer customer, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("创建客户: operatorId={}, customerName={}", operatorId, customer.getName());
        customer.setId(UUID.randomUUID());
        customerMapper.insert(customer);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客户创建成功"));
    }

    // 查询所有用户
    @GetMapping("/all")
    public ResponseEntity<?> findAll() {
        List<Customer> customers = customerMapper.findAll();
        return ResponseEntity.ok(Map.of("code", 200, "message", "查询成功", "data", customers));
    }

    // 查询单个客户
    @GetMapping("/{id}/find")
    public ResponseEntity<?> findById(@PathVariable UUID id) {
        try {
            Customer customer = customerMapper.findById(id);
            return ResponseEntity.ok(Map.of("code", 200, "data", customer));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "客户不存在"));
        }
    }

    // 更新客户
    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Customer customer, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("更新客户: operatorId={}, customerId={}", operatorId, id);
        customer.setId(id);
        customerMapper.update(customer);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客户更新成功"));
    }

    // 删除客户
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable UUID id, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("删除客户: operatorId={}, customerId={}", operatorId, id);
        customerMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客户删除成功"));
    }
}
