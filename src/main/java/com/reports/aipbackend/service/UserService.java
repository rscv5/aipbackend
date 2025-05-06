package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 * 处理用户相关的业务逻辑
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    public User login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        
        // 查找用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            logger.warn("User not found: {}", username);
            return null;
        }

        logger.info("Found user: {}, password hash: {}", username, user.getPasswordHash());

        // 验证密码
        if (user.getPasswordHash() != null) {
            // 如果密码是明文，直接比较
            if (!user.getPasswordHash().startsWith("$2a$")) {
                logger.info("Password is in plain text format");
                if (user.getPasswordHash().equals(password)) {
                    // 登录成功后，将密码更新为BCrypt格式
                    String encodedPassword = passwordEncoder.encode(password);
                    user.setPasswordHash(encodedPassword);
                    userMapper.updatePassword(user.getUserId(), encodedPassword);
                    logger.info("Password updated to BCrypt format for user: {}", username);
                    return user;
                }
                logger.warn("Invalid password for user: {}", username);
                return null;
            }
            
            // 如果是BCrypt格式，使用passwordEncoder验证
            logger.info("Password is in BCrypt format");
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                logger.warn("Invalid password for user: {}", username);
                return null;
            }
        } else {
            logger.warn("No password hash found for user: {}", username);
            return null;
        }

        logger.info("Login successful for user: {}", username);
        return user;
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息
     */
    public User findByUsername(String username) {
        logger.info("Finding user by username: {}", username);
        return userMapper.findByUsername(username);
    }

    /**
     * 根据OpenID查找用户
     * @param openid 微信OpenID
     * @return 用户信息
     */
    public User findByOpenid(String openid) {
        logger.info("Finding user by openid: {}", openid);
        return userMapper.findByOpenid(openid);
    }

    /**
     * 保存用户信息
     * @param user 用户信息
     * @return 保存后的用户信息
     */
    public User save(User user) {
        logger.info("Saving user: {}", user.getUsername());
        if (user.getUserId() == null) {
            // 新用户，设置默认密码
            user.setPasswordHash(passwordEncoder.encode("123456"));
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }
} 