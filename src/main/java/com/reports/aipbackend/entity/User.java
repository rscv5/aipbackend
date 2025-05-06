package com.reports.aipbackend.entity;

import lombok.Data;

@Data
public class User {
    private Integer userId;
    private String openid;
    private String username;
    private String passwordHash;
    private String role;
    private String phoneNumber;
} 