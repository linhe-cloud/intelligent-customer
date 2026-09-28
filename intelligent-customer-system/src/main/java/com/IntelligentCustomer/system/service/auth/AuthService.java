package com.IntelligentCustomer.system.service.auth;

import com.IntelligentCustomer.system.repository.mapper.StaffMapper;
import com.IntelligentCustomer.system.repository.mapper.CustomerMapper;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.IntelligentCustomer.framework.config.security.JwtUtil;
import com.IntelligentCustomer.system.domain.entity.Customer;
import com.IntelligentCustomer.system.domain.entity.Staff;
import com.IntelligentCustomer.system.domain.dto.auth.LoginRequest;
import com.IntelligentCustomer.system.domain.vo.auth.LoginResponse;
import com.IntelligentCustomer.system.domain.dto.auth.RegisterRequest;
import com.IntelligentCustomer.common.exception.BusinessException;

@Service
public class AuthService {

    private final CustomerMapper customerMapper;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    public AuthService(CustomerMapper customerMapper, StaffMapper staffMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.customerMapper = customerMapper;
        this.staffMapper = staffMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // 登录
    public LoginResponse login(LoginRequest request) {
        if ("STAFF".equalsIgnoreCase(request.getUserType())) {
            return loginAsStaff(request);
        } else {
            return loginAsCustomer(request);
        }
    }

    // STAFF 登录
    private LoginResponse loginAsStaff(LoginRequest request) {
        Staff staff = staffMapper.findByUsername(request.getUsername());
        if (staff == null) throw new BusinessException("用户名或密码错误");
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), staff.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 更新最后登陆时间（用 id）
        staffMapper.updateLastLogin(staff.getId());

        // 生成 Token（subject = id）
        String token = jwtUtil.generateToken(staff.getId().toString(), "STAFF", staff.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(staff.getId().toString());
        response.setUsertype("STAFF");
        response.setRole(staff.getRole());
        response.setExpiresIn(jwtUtil.getExpiration(token));
        return response;
    }
    
    // CUSTOMER 登录
    private LoginResponse loginAsCustomer(LoginRequest request) {

        Customer customer = customerMapper.findByUsername(request.getUsername());
        if (customer == null) throw new BusinessException("用户名或密码错误");
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 更新最后登陆时间（用 id）
        customerMapper.updateLastLogin(customer.getId());

        // 生成 Token（subject = id）
        String token = jwtUtil.generateToken(customer.getId().toString(), "CUSTOMER", "CUSTOMER");

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(customer.getId().toString());
        response.setUsertype("CUSTOMER");
        response.setRole("CUSTOMER");
        response.setExpiresIn(jwtUtil.getExpiration(token));
        return response;
    }

    // 注册
    public void register(RegisterRequest request) {
        if ("STAFF".equalsIgnoreCase(request.getUserType())) {
            registerAsStaff(request);
        } else {
            registerAsCustomer(request);
        }
    }

    private void registerAsCustomer(RegisterRequest request) {
        // 检查用户名是否已存在
        if (customerMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setUsername(request.getUsername());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setStatus("ACTIVE");
        customerMapper.insert(customer);
    }

    private void registerAsStaff(RegisterRequest request) {
        // 检查用户名是否已存在
        if (staffMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setUsername(request.getUsername());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staff.setName(request.getName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole() != null ? request.getRole() : "AGENT");
        staff.setStatus("ACTIVE");
        staffMapper.insert(staff);
    }
}
