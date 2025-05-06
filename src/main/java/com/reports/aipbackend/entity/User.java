package com.reports.aipbackend.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
public class User implements UserDetails {
    private Integer userId;
    private String openid;
    private String username;
    private String passwordHash;
    private String role;
    private String phoneNumber;

    /**
     * 获取用户权限列表
     * 根据用户角色返回对应的权限
     * @return 权限列表
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 根据角色返回对应的权限
        switch (role) {
            case "片区长":
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "网格员":
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GRID"));
            default:
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
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