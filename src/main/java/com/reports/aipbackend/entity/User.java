package com.reports.aipbackend.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
public class User implements UserDetails {
    private Integer userId;
    private String openid;
    private String username;
    private String passwordHash;
    private String role;
    private String phoneNumber;
    private Boolean isSuperAdmin;
    private Boolean isDeleted;

    /**
     * 获取用户权限列表
     * 根据用户角色返回对应的权限
     * @return 权限列表
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 根据角色返回对应的权限
        switch (role) {
            case "片区长":
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                // 如果是超级管理员，添加额外权限
                if (Boolean.TRUE.equals(isSuperAdmin)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
                break;
            case "网格员":
                authorities.add(new SimpleGrantedAuthority("ROLE_GRID"));
                break;
            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
} 