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

/**
 * 认证服务类，处理用户登录、注册等认证相关功能
 */
@Service
public class AuthService {

    // 客户数据访问接口
    private final CustomerMapper customerMapper;
    // 员工数据访问接口
    private final StaffMapper staffMapper;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;
    // JWT工具类
    private final JwtUtil jwtUtil;
    /**
     * 构造函数，注入所需依赖
     * @param customerMapper 客户数据访问接口
     * @param staffMapper 员工数据访问接口
     * @param passwordEncoder 密码编码器
     * @param jwtUtil JWT工具类
     */
    public AuthService(CustomerMapper customerMapper, StaffMapper staffMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.customerMapper = customerMapper;
        this.staffMapper = staffMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

/**
 * 登录方法，根据用户类型进行不同的登录处理
 * @param request 登录请求对象，包含用户名、密码和用户类型等信息
 * @return LoginResponse 登录响应对象，包含登录结果和相关信息
 * @throws BusinessException 当用户类型不合法时抛出业务异常
 */
    // 登录
    public LoginResponse login(LoginRequest request) {
    // 验证请求参数的合法性
        validateRequest(request);

    // 判断用户类型是否为员工
        if ("STAFF".equalsIgnoreCase(request.getUserType())) {
        // 执行员工登录流程
            return loginAsStaff(request);
        } else if ("CUSTOMER".equalsIgnoreCase(request.getUserType())) {
            return loginAsCustomer(request);
        }

        throw new BusinessException("用户类型不合法");
    }

/**
 * 员工登录方法
 * @param request 登录请求对象，包含用户名和密码信息
 * @return LoginResponse 登录响应对象，包含token、用户ID、用户类型、角色和过期时间
 * @throws BusinessException 当用户名不存在、账号被禁用或密码错误时抛出业务异常
 */
    // STAFF 登录
    private LoginResponse loginAsStaff(LoginRequest request) {
    // 根据用户名查找员工信息
        Staff staff = staffMapper.findByUsername(request.getUsername());
    // 如果员工不存在，抛出用户名或密码错误异常
        if (staff == null) {
            throw new BusinessException("用户名或密码错误", 401);
        }
    // 检查员工账号状态是否为激活，如果不是则抛出账号已禁用异常
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new BusinessException("账号已禁用", 403);
        }
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), staff.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误", 401);
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
        if (customer == null) {
            throw new BusinessException("用户名或密码错误", 401);
        }
        if (!"ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException("账号已禁用", 403);
        }
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误", 401);
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
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword())
                || isBlank(request.getUserType())) {
            throw new BusinessException("注册参数不完整");
        }

        if (!"CUSTOMER".equalsIgnoreCase(request.getUserType())) {
            if ("STAFF".equalsIgnoreCase(request.getUserType())) {
                throw new BusinessException("公共注册仅支持客户账号，STAFF/ADMIN账号请由管理员创建", 403);
            }
            throw new BusinessException("用户类型不合法");
        }

        registerAsCustomer(request);
    }

/**
 * 注册客户方法
 * @param request 包含注册信息的请求对象，包含用户名、密码、姓名、邮箱和电话等信息
 */
    private void registerAsCustomer(RegisterRequest request) {
        // 检查用户名是否已存在，如果存在则抛出业务异常
        if (customerMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在", 409);
        }
        // 创建新的客户对象
        Customer customer = new Customer();
        // 设置客户ID为随机生成的UUID
        customer.setId(UUID.randomUUID());
        // 设置客户用户名
        customer.setUsername(request.getUsername());
        // 设置客户密码的哈希值，对密码进行加密处理
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // 设置客户姓名
        customer.setName(request.getName());
        // 设置客户邮箱
        customer.setEmail(request.getEmail());
        // 设置客户电话
        customer.setPhone(request.getPhone());
        // 设置客户状态为"ACTIVE"，表示账户已激活
        customer.setStatus("ACTIVE");
        // 将新客户信息插入数据库
        customerMapper.insert(customer);
    }

/**
 * 注册员工账号
 * @param request 包含注册信息的请求对象，包含用户名、密码、姓名、邮箱、电话和角色等信息
 * @throws BusinessException 当用户名已存在时抛出，状态码为409
 */
    private void registerAsStaff(RegisterRequest request) {
        // 检查用户名是否已存在
        if (staffMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在", 409);
        }
        // 创建新的员工对象
        Staff staff = new Staff();
        // 设置员工唯一标识
        staff.setId(UUID.randomUUID());
        // 设置用户名
        staff.setUsername(request.getUsername());
        // 设置加密后的密码
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // 设置员工姓名
        staff.setName(request.getName());
        // 设置员工邮箱
        staff.setEmail(request.getEmail());
        // 设置员工电话
        staff.setPhone(request.getPhone());
        // 设置员工角色，如果未指定则默认为AGENT
        staff.setRole(request.getRole() != null ? request.getRole() : "AGENT");
        // 设置员工状态为激活
        staff.setStatus("ACTIVE");
        // 将员工信息插入数据库
        staffMapper.insert(staff);
    }

    private void validateRequest(LoginRequest request) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword())
                || isBlank(request.getUserType())) {
            throw new BusinessException("登录参数不完整");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
