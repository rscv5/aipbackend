package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE username = #{username} AND password_hash = #{passwordHash}")
    User findByUsernameAndPassword(@Param("username") String username, @Param("passwordHash") String passwordHash);

    @Select("SELECT * FROM users WHERE openid = #{openid}")
    User findByOpenid(@Param("openid") String openid);
} 