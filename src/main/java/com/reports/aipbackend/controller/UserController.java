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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;

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
            String openId = getOpenidFromWechat(code);
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

    // 调用微信API获取openid
    private String getOpenidFromWechat(String code) {
        try {
            // 构建请求URL
            String url = "https://api.weixin.qq.com/sns/jscode2session";
            String appid = System.getenv("WECHAT_APPID"); // 从环境变量中获取 AppID
            String secret = System.getenv("WECHAT_SECRET"); // 从环境变量中获取 AppSecret
            String grantType = "authorization_code";

            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("appid", appid);
            params.put("secret", secret);
            params.put("js_code", code);
            params.put("grant_type", grantType);

            // 发送请求
            String response = sendGetRequest(url, params);

            // 解析响应
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("openid")) {
                return jsonResponse.getString("openid");
            } else {
                logger.error("Failed to get openid from WeChat API: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting openid from WeChat API: ", e);
            return null;
        }
    }

    // 发送GET请求
    private String sendGetRequest(String url, Map<String, String> params) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1);

        URL urlObj = new URL(urlBuilder.toString());
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new Exception("GET request failed with response code: " + responseCode);
        }
    }
} 