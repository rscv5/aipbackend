package com.reports.aipbackend.service;

import com.reports.aipbackend.entity.User;
import com.reports.aipbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义用户详情服务
 * 实现 UserDetailsService 接口，用于加载用户详情
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 根据用户名加载用户详情
     * @param username 用户名
     * @return 用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先尝试通过手机号查找用户
        User user = userMapper.findByPhoneNumber(username);
        if (user == null) {
            // 如果手机号查找失败，尝试通过用户名查找
            user = userMapper.findByUsername(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return createUserDetails(user);
    }

    /**
     * 根据用户ID加载用户详情
     * @param userId 用户ID
     * @return 用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出异常
     */
    public UserDetails loadUserById(Integer userId) throws UsernameNotFoundException {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
        return createUserDetails(user);
    }

    /**
     * 根据 openid 加载用户详情
     * @param openid 微信用户唯一标识
     * @return 用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出异常
     */
    public UserDetails loadUserByOpenid(String openid) throws UsernameNotFoundException {
        User user = userMapper.findByOpenid(openid);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + openid);
        }
        return createUserDetails(user);
    }

    /**
     * 创建 UserDetails 对象
     * @param user 用户实体
     * @return UserDetails 对象
     */
    private UserDetails createUserDetails(User user) {
        // 如果用户名为空，使用 openid 作为用户名
        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = user.getOpenid();
        }
        
        // 如果密码为空，使用一个默认的密码（这种情况不应该发生，但为了安全起见）
        String password = user.getPasswordHash();
        if (password == null || password.trim().isEmpty()) {
            password = "{noop}default_password";
        }

        // 创建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 根据角色返回对应的权限
        switch (user.getRole()) {
            case "片区长":
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                // 如果是超级管理员，添加额外权限
                if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
                break;
            case "网格员":
                authorities.add(new SimpleGrantedAuthority("ROLE_GRID"));
                break;
            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new org.springframework.security.core.userdetails.User(
            username,
            password,
            authorities
        );
    }
} 