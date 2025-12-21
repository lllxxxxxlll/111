package com.education.ai.repository;

import com.education.ai.model.User;
import com.education.ai.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 提供用户实体的CRUD操作和自定义查询方法
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * 查找所有活跃用户
     */
    List<User> findByActiveTrue();
    
    /**
     * 根据角色查找用户
     */
    List<User> findByRole(UserRole role);
    
    /**
     * 根据角色和活跃状态查找用户
     */
    List<User> findByRoleAndActive(UserRole role, Boolean active);
    
    /**
     * 查找指定时间之后创建的用户
     */
    List<User> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 查找指定时间之后登录的用户
     */
    List<User> findByLastLoginAtAfter(LocalDateTime dateTime);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 统计活跃用户数量
     */
    long countByActiveTrue();
    
    /**
     * 根据角色统计用户数量
     */
    long countByRole(UserRole role);
    
    /**
     * 更新用户最后登录时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.userId = :userId")
    int updateLastLoginTime(@Param("userId") String userId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * 批量激活用户
     */
    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.userId IN :userIds")
    int activateUsers(@Param("userIds") List<String> userIds);
    
    /**
     * 批量停用用户
     */
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.userId IN :userIds")
    int deactivateUsers(@Param("userIds") List<String> userIds);
    
    /**
     * 查找长时间未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND " +
           "(u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate)")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 根据用户名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.fullName LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}