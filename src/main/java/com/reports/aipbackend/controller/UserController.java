package com.reports.aipbackend.controller;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping(value = "/grid/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> gridLogin(@RequestBody LoginRequest request) {
        logger.info("Received grid login request: username={}", request.getUsername());
        try {
            System.out.println("asdfasdfasdfasdfasdfasdf"+request.getUsername());
            System.out.println("asdfasdfasdfasdfasdfasdf"+request.getPassword());
            User user = userService.login(request.getUsername(), request.getPassword());
            System.out.println("asdfasdfasdfasdfasdfasdf"+user.getRole().toString());
            if (user != null && ("网格员".equals(user.getRole()) || "片区长".equals(user.getRole()))) {
                System.out.println("asdfasdfasdfasdfasdfasdf"+user.getRole().toString());
                user.setPasswordHash(null);
                logger.info("Grid login successful for user: {}", user.getUsername());
                return ResponseEntity.ok(user);
            } else {
                logger.warn("Grid login failed: invalid credentials or role for username: {}", request.getUsername());
                return ResponseEntity.badRequest().body("账号或密码错误，或用户不是网格员/片区长");
            }
        } catch (Exception e) {
            logger.error("Grid login error: ", e);
            return ResponseEntity.internalServerError().body("服务器内部错误");
        }
    }

    @PostMapping(value = "/user/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userLogin(@RequestBody UserLoginRequest request) {
        logger.info("Received user login request: openid={}", request.getOpenid());
        try {
            User user = userService.findByOpenid(request.getOpenid());
            if (user != null) {
                user.setPasswordHash(null);
                logger.info("User login successful for openid: {}", request.getOpenid());
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User not found for openid: {}", request.getOpenid());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("User login error: ", e);
            return ResponseEntity.internalServerError().body("服务器内部错误");
        }
    }

    @GetMapping(value = "/user/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserInfo(@RequestParam String openid) {
        logger.info("Getting user info for openid: {}", openid);
        try {
            User user = userService.findByOpenid(openid);
            if (user != null) {
                user.setPasswordHash(null);
                logger.info("User info found for openid: {}", openid);
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User not found for openid: {}", openid);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting user info: ", e);
            return ResponseEntity.internalServerError().body("服务器内部错误");
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class UserLoginRequest {
        private String openid;

        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }
    }
} 