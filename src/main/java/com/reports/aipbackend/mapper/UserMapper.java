package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据用户名和密码查找用户
     * @param username 用户名
     * @param passwordHash 密码哈希
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND password_hash = #{passwordHash}")
    User findByUsernameAndPassword(@Param("username") String username, @Param("passwordHash") String passwordHash);

    /**
     * 根据OpenID查找用户
     * @param openid 微信OpenID
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE openid = #{openid}")
    User findByOpenid(String openid);

    /**
     * 插入新用户
     * @param user 用户信息
     * @return 影响的行数
     */
    @Insert("INSERT INTO users (openid, username, password_hash, role, phone_number) " +
            "VALUES (#{openid}, #{username}, #{passwordHash}, #{role}, #{phoneNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 影响的行数
     */
    @Update("UPDATE users SET username = #{username}, password_hash = #{passwordHash}, " +
            "role = #{role}, phone_number = #{phoneNumber} WHERE user_id = #{userId}")
    int update(User user);

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param passwordHash 密码哈希
     */
    @Update("UPDATE users SET password_hash = #{passwordHash} WHERE user_id = #{userId}")
    void updatePassword(@Param("userId") Integer userId, @Param("passwordHash") String passwordHash);

    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    User findById(Integer userId);
} 