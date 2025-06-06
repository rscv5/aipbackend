package com.reports.aipbackend.controller;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/grid-users")
public class GridUserController {

    @Autowired
    private UserService userService;

    /**
     * 创建网格员账号（仅超级管理员可用）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> createGridUser(@RequestBody User user) {
        user.setRole("网格员");
        User createdUser = userService.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "网格员账号创建成功");
        response.put("data", createdUser);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除网格员账号（仅超级管理员可用）
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> deleteGridUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "网格员账号已停用");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有网格员列表（所有片区长可用）
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getAllGridUsers() {
        List<User> gridUsers = userService.getUsersByRole("网格员");
        // 移除敏感信息
        gridUsers.forEach(user -> user.setPasswordHash(null));
        return ResponseEntity.ok(gridUsers);
    }

    /**
     * 重置网格员密码（仅超级管理员可用）
     */
    @PostMapping(value = "/{userId}/reset-password", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Integer userId) {
        userService.resetPassword(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "密码已重置为默认密码");
        response.put("code", 200);
        return ResponseEntity.ok(response);
    }

    /**
     * 修改网格员信息（仅超级管理员可用）
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateGridUser(@PathVariable Integer userId, @RequestBody User user) {
        user.setUserId(userId);
        user.setRole("网格员"); // 确保角色不被修改
        User updatedUser = userService.save(user);
        updatedUser.setPasswordHash(null);
        return ResponseEntity.ok(updatedUser);
    }
} 