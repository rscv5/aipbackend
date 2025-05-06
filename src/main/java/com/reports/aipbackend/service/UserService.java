package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User login(String username, String password) {
        try {
            logger.info("尝试登录用户: {}", username);
            User user = userMapper.findByUsername(username);
            if (user != null) {
                // 检查密码是否匹配
                if (password.equals(user.getPasswordHash())) {
                    logger.info("用户 {} 登录成功", username);
                    return user;
                }
                logger.warn("用户 {} 密码错误", username);
            } else {
                logger.warn("未找到用户: {}", username);
            }
            return null;
        } catch (Exception e) {
            logger.error("登录过程中发生错误: ", e);
            throw e;
        }
    }

    public User findByUsername(String username) {
        try {
            logger.info("查找用户: {}", username);
            return userMapper.findByUsername(username);
        } catch (Exception e) {
            logger.error("查找用户时发生错误: ", e);
            throw e;
        }
    }

    public User findByOpenid(String openid) {
        try {
            logger.info("通过openid查找用户: {}", openid);
            return userMapper.findByOpenid(openid);
        } catch (Exception e) {
            logger.error("通过openid查找用户时发生错误: ", e);
            throw e;
        }
    }
} 