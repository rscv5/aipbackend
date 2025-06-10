package com.reports.aipbackend.controller;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.service.UserService;
import com.reports.aipbackend.common.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/grid/login")
    public ResponseEntity<?> gridLogin(@RequestBody Map<String, String> loginRequest) {
        logger.info("Login attempt - username/phone: {}", loginRequest.get("username"));
        
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || password == null) {
            logger.warn("Login failed - missing username/phone or password");
            return ResponseEntity.badRequest().body("请输入手机号和密码");
            }

            User user = userService.login(username, password);

        if (user != null && ("网格员".equals(user.getRole()) || "片区长".equals(user.getRole()))) {
            logger.info("Login successful - username/phone: {}, role: {}", username, user.getRole());

            // 生成JWT令牌
            String token = jwtUtils.generateToken(user);

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userInfo", user);

            return ResponseEntity.ok(response);
        } else {
            String errorMsg = user == null ? "手机号或密码错误" : "无权限登录";
            logger.warn("Login failed - {} for user: {}", errorMsg, username);
            return ResponseEntity.badRequest().body(errorMsg);
        }
    }

    @PostMapping("/user/login")
    public ResponseEntity<?> userLogin(@RequestBody Map<String, String> loginRequest) {
        try {
            String code = loginRequest.get("code");
            if (code == null) {
                logger.warn("Login failed - code is null");
                return ResponseEntity.badRequest().body("微信登录code不能为空");
            }

            // TODO: 调用微信API获取openid
            String openId = "test_openid"; // 临时使用测试openid
            logger.info("WeChat login attempt - openid: {}", openId);

            User user = userService.findByOpenid(openId);
            if (user == null) {
                // 自动注册新用户
                logger.info("Creating new user for openid: {}", openId);
                user = new User();
                user.setOpenid(openId);
                user.setRole("普通用户");
                user = userService.save(user);
            }

            // 生成JWT token
            String token = jwtUtils.generateToken(user);
            logger.info("Generated JWT token for user: {}", user.getUsername());

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole());
            response.put("userId", user.getUserId());

            logger.info("Login successful for user: {}, role: {}", user.getUsername(), user.getRole());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("User login error: ", e);
            return ResponseEntity.internalServerError().body("服务器内部错误");
        }
    }

    @GetMapping(value = "/user/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid authorization header");
                return ResponseEntity.status(403).body("无效的认证信息");
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtils.getClaimsFromToken(token);
            
            if (claims == null) {
                logger.warn("Invalid token");
                return ResponseEntity.status(403).body("无效的token");
            }

            Integer userId = claims.get("userId", Integer.class);
            User user = userService.findByUserId(userId);
            
            if (user == null) {
                logger.warn("User not found for userId: {}", userId);
                return ResponseEntity.status(403).body("用户不存在");
            }

            // 不返回敏感信息
            user.setPasswordHash(null);

            // 构建响应数据，确保包含 isSuperAdmin 字段
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("openid", user.getOpenid());
            response.put("isSuperAdmin", user.getIsSuperAdmin());

            logger.info("User info found for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user info: ", e);
            return ResponseEntity.status(403).body("获取用户信息失败");
        }
    }
} 