package com.reports.aipbackend.controller;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.service.CustomUserDetailsService;
import com.reports.aipbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户认证相关的请求
 */
@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/auth/current-user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            user.setPasswordHash(null); // 不返回密码
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.badRequest().body("未登录");
    }

    /**
     * 退出登录
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().body("退出成功");
    }
} 