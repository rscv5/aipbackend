package com.reports.aipbackend.mapper;

import com.reports.aipbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
    User findByUsername(String username);

    /**
     * 根据手机号查找用户
     * @param phoneNumber 手机号
     * @return 用户信息
     */
    User findByPhoneNumber(String phoneNumber);

    /**
     * 根据用户名和密码查找用户
     * @param username 用户名
     * @param passwordHash 密码哈希
     * @return 用户信息
     */
    User findByUsernameAndPassword(@Param("username") String username, @Param("passwordHash") String passwordHash);

    /**
     * 根据OpenID查找用户
     * @param openid 微信OpenID
     * @return 用户信息
     */
    User findByOpenid(String openid);

    /**
     * 插入新用户
     * @param user 用户信息
     * @return 影响的行数
     */
    int insert(User user);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 影响的行数
     */
    int update(User user);

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param passwordHash 密码哈希
     */
    void updatePassword(@Param("userId") Integer userId, @Param("passwordHash") String passwordHash);

    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User findById(Integer userId);

    /**
     * 根据角色查找用户
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> findByRole(String role);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteById(Integer userId);
} 