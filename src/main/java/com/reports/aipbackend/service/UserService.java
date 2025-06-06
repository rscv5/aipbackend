package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务类
 * 处理用户相关的业务逻辑
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_PASSWORD = "Mf654321";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        // 如果是片区长，检查并更新密码格式
        if ("片区长".equals(user.getRole())) {
            checkAndUpdateAreaManagerPassword(username);
            // 重新获取用户信息（因为密码可能已更新）
            user = userMapper.findByUsername(username);
        }

        // 验证密码
        if (user.getPasswordHash() != null) {
            logger.info("Stored password hash: {}", user.getPasswordHash());
            logger.info("Input password: {}", password);
            boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
            logger.info("Password match result: {}", matches);
            if (!matches) {
                    logger.warn("Invalid password for user: {}", username);
                    return null;
            }
        } else {
            logger.warn("No password hash found for user: {}", username);
            return null;
        }

        // 登录成功，返回用户信息
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
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户信息
     */
    public User findByUserId(Integer userId) {
        logger.info("Finding user by userId: {}", userId);
        return userMapper.findById(userId);
    }

    /**
     * 保存用户信息
     * @param user 用户信息
     * @return 保存后的用户信息
     */
    public User save(User user) {
        logger.info("Saving user: {}", user.getUsername());
        if (user.getUserId() == null) {
            // 新用户，设置默认密码并加密
            String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
            logger.info("新用户 - 用户名: {}, 原始密码: {}, 加密后: {}", user.getUsername(), DEFAULT_PASSWORD, encodedPassword);
            user.setPasswordHash(encodedPassword);
            if (user.getRole().equals("网格员")) {
                user.setOpenid("grid_" + user.getUsername());
            }
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }

    /**
     * 根据openid获取用户信息
     * @param openid 用户openid
     * @return 用户信息
     */
    public User getUserByOpenid(String openid) {
        logger.info("根据openid查询用户: openid={}", openid);
        if (openid == null || openid.trim().isEmpty()) {
            return null;
        }
        return userMapper.findByOpenid(openid);
    }

    /**
     * 更新用户信息
     * @param user 用户对象
     */
    public void updateUser(User user) {
        userMapper.update(user);
    }

    /**
     * 根据角色获取用户列表
     * @param role 用户角色
     * @return 用户列表
     */
    public List<User> getUsersByRole(String role) {
        logger.info("根据角色查询用户列表: role={}", role);
        return userMapper.findByRole(role);
    }

    /**
     * 删除用户
     * @param userId 用户ID
     */
    public void deleteUser(Integer userId) {
        logger.info("删除用户: userId={}", userId);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!"网格员".equals(user.getRole())) {
            throw new RuntimeException("只能删除网格员账号");
        }
        userMapper.deleteById(userId);
    }

    /**
     * 重置用户密码
     * @param userId 用户ID
     */
    public void resetPassword(Integer userId) {
        logger.info("重置用户密码: userId={}", userId);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!"网格员".equals(user.getRole())) {
            throw new RuntimeException("只能重置网格员密码");
        }
        // 对默认密码进行加密
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        logger.info("重置密码 - 用户: {}, 原始密码: {}, 加密后: {}", user.getUsername(), DEFAULT_PASSWORD, encodedPassword);
        userMapper.updatePassword(userId, encodedPassword);
    }

    /**
     * 初始化或更新用户密码
     * @param userId 用户ID
     * @param password 密码
     */
    public void initializePassword(Integer userId, String password) {
        logger.info("初始化用户密码: userId={}", userId);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 对密码进行加密
        String encodedPassword = passwordEncoder.encode(password);
        logger.info("初始化密码 - 用户: {}, 原始密码: {}, 加密后: {}", user.getUsername(), password, encodedPassword);
        userMapper.updatePassword(userId, encodedPassword);
    }

    /**
     * 检查并更新片区长密码
     * @param username 用户名
     */
    public void checkAndUpdateAreaManagerPassword(String username) {
        logger.info("检查片区长密码: {}", username);
        User user = userMapper.findByUsername(username);
        if (user != null && "片区长".equals(user.getRole())) {
            // 如果密码不是BCrypt格式，则更新为加密格式
            if (user.getPasswordHash() == null || !user.getPasswordHash().startsWith("$2a$")) {
                logger.info("更新片区长密码为加密格式: {}", username);
                initializePassword(user.getUserId(), "123456");
            }
        }
    }
} 