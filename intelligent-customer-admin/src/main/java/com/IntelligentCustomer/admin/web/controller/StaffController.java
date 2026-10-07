package com.IntelligentCustomer.admin.web.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.IntelligentCustomer.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.entity.Staff;
import com.IntelligentCustomer.system.repository.mapper.StaffMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff")
public class StaffController {

    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);

    private final StaffMapper staffMapper;

    public StaffController(StaffMapper staffMapper) {
        this.staffMapper = staffMapper;
    }

    // 新增客服人员
    @PostMapping("/add")
    public ResponseEntity<?> create(@RequestBody Staff staff, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("创建客服人员: operatorId={}, staffName={}", operatorId, staff.getName());
        staff.setId(UUID.randomUUID());
        staffMapper.insert(staff);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客服人员创建成功"));
    }

    // 查询所有客服人员
    @GetMapping("/all")
    public ResponseEntity<?> findAll() {
        List<Staff> staffList = staffMapper.findAll();
        return ResponseEntity.ok(Map.of("code", 200, "data", staffList, "totalCount", staffList.size()));
    }

    // 按角色查询
    @GetMapping("/role/{role}/find")
    public ResponseEntity<?> findByRole(@PathVariable String role) {
        List<Staff> staffList = staffMapper.findByRole(role);
        return ResponseEntity.ok(Map.of("code", 200, "data", staffList, "totalCount", staffList.size()));
    }

    // 查询单个
    @GetMapping("/{id}/find")
    public ResponseEntity<?> findById(@PathVariable UUID id) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) {
            throw new BusinessException("客服人员不存在", 404);
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", staff));
    }

    // 更新
    @PutMapping("/{id}/update")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Staff staff, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("更新客服人员: operatorId={}, staffId={}", operatorId, id);
        staff.setId(id);
        staffMapper.update(staff);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客服人员更新成功"));
    }

    // 删除
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable UUID id, Principal principal) {
        String operatorId = principal != null ? principal.getName() : "unknown";
        logger.info("删除客服人员: operatorId={}, staffId={}", operatorId, id);
        staffMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "客服人员删除成功"));
    }
}
