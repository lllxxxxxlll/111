package com.education.ai.repository;

import com.education.ai.model.User;
import com.education.ai.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户Repository单元测试
 * 测试用户数据访问层的CRUD操作和自定义查询方法
 */
class UserRepositoryTest extends RepositoryTestBase {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testSaveAndFindUser() {
        // 创建测试用户
        User user = new User("user123", "testuser", "test@example.com");
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setFullName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        
        // 保存用户
        User savedUser = userRepository.save(user);
        flushAndClear();
        
        // 验证保存结果
        assertNotNull(savedUser);
        assertEquals("user123", savedUser.getUserId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals(UserRole.STUDENT, savedUser.getRole());
        assertTrue(savedUser.getActive());
        
        // 测试根据ID查找
        Optional<User> foundUser = userRepository.findById("user123");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }
    
    @Test
    void testFindByUsername() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 测试根据用户名查找
        Optional<User> foundUser = userRepository.findByUsername("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals("user123", foundUser.get().getUserId());
        
        // 测试不存在的用户名
        Optional<User> notFound = userRepository.findByUsername("nonexistent");
        assertFalse(notFound.isPresent());
    }
    
    @Test
    void testFindByEmail() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 测试根据邮箱查找
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");
        assertTrue(foundUser.isPresent());
        assertEquals("user123", foundUser.get().getUserId());
        
        // 测试不存在的邮箱
        Optional<User> notFound = userRepository.findByEmail("nonexistent@example.com");
        assertFalse(notFound.isPresent());
    }
    
    @Test
    void testFindByUsernameOrEmail() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 测试根据用户名查找
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("testuser", "wrong@email.com");
        assertTrue(foundByUsername.isPresent());
        assertEquals("user123", foundByUsername.get().getUserId());
        
        // 测试根据邮箱查找
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("wronguser", "test@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals("user123", foundByEmail.get().getUserId());
        
        // 测试都不匹配
        Optional<User> notFound = userRepository.findByUsernameOrEmail("wronguser", "wrong@email.com");
        assertFalse(notFound.isPresent());
    }
    
    @Test
    void testFindByActiveTrue() {
        // 创建活跃和非活跃用户
        User activeUser = createTestUser("user1", "active", "active@example.com");
        activeUser.setActive(true);
        
        User inactiveUser = createTestUser("user2", "inactive", "inactive@example.com");
        inactiveUser.setActive(false);
        
        userRepository.save(activeUser);
        userRepository.save(inactiveUser);
        flushAndClear();
        
        // 测试查找活跃用户
        List<User> activeUsers = userRepository.findByActiveTrue();
        assertEquals(1, activeUsers.size());
        assertEquals("active", activeUsers.get(0).getUsername());
    }
    
    @Test
    void testFindByRole() {
        // 创建不同角色的用户
        User student = createTestUser("user1", "student", "student@example.com");
        student.setRole(UserRole.STUDENT);
        
        User teacher = createTestUser("user2", "teacher", "teacher@example.com");
        teacher.setRole(UserRole.TEACHER);
        
        User admin = createTestUser("user3", "admin", "admin@example.com");
        admin.setRole(UserRole.ADMIN);
        
        userRepository.save(student);
        userRepository.save(teacher);
        userRepository.save(admin);
        flushAndClear();
        
        // 测试根据角色查找
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        assertEquals(1, students.size());
        assertEquals("student", students.get(0).getUsername());
        
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        assertEquals(1, teachers.size());
        assertEquals("teacher", teachers.get(0).getUsername());
    }
    
    @Test
    void testExistsByUsername() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 测试用户名存在性检查
        assertTrue(userRepository.existsByUsername("testuser"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
    }
    
    @Test
    void testExistsByEmail() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 测试邮箱存在性检查
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }
    
    @Test
    void testCountByActiveTrue() {
        // 创建活跃和非活跃用户
        User activeUser1 = createTestUser("user1", "active1", "active1@example.com");
        activeUser1.setActive(true);
        
        User activeUser2 = createTestUser("user2", "active2", "active2@example.com");
        activeUser2.setActive(true);
        
        User inactiveUser = createTestUser("user3", "inactive", "inactive@example.com");
        inactiveUser.setActive(false);
        
        userRepository.save(activeUser1);
        userRepository.save(activeUser2);
        userRepository.save(inactiveUser);
        flushAndClear();
        
        // 测试活跃用户计数
        long activeCount = userRepository.countByActiveTrue();
        assertEquals(2, activeCount);
    }
    
    @Test
    void testCountByRole() {
        // 创建不同角色的用户
        User student1 = createTestUser("user1", "student1", "student1@example.com");
        student1.setRole(UserRole.STUDENT);
        
        User student2 = createTestUser("user2", "student2", "student2@example.com");
        student2.setRole(UserRole.STUDENT);
        
        User teacher = createTestUser("user3", "teacher", "teacher@example.com");
        teacher.setRole(UserRole.TEACHER);
        
        userRepository.save(student1);
        userRepository.save(student2);
        userRepository.save(teacher);
        flushAndClear();
        
        // 测试角色用户计数
        long studentCount = userRepository.countByRole(UserRole.STUDENT);
        assertEquals(2, studentCount);
        
        long teacherCount = userRepository.countByRole(UserRole.TEACHER);
        assertEquals(1, teacherCount);
    }
    
    @Test
    void testUpdateLastLoginTime() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 更新最后登录时间
        LocalDateTime loginTime = LocalDateTime.now();
        int updatedRows = userRepository.updateLastLoginTime("user123", loginTime);
        assertEquals(1, updatedRows);
        
        flushAndClear();
        
        // 验证更新结果
        Optional<User> updatedUser = userRepository.findById("user123");
        assertTrue(updatedUser.isPresent());
        assertNotNull(updatedUser.get().getLastLoginAt());
    }
    
    @Test
    void testActivateUsers() {
        // 创建非活跃用户
        User user1 = createTestUser("user1", "user1", "user1@example.com");
        user1.setActive(false);
        
        User user2 = createTestUser("user2", "user2", "user2@example.com");
        user2.setActive(false);
        
        userRepository.save(user1);
        userRepository.save(user2);
        flushAndClear();
        
        // 批量激活用户
        List<String> userIds = List.of("user1", "user2");
        int updatedRows = userRepository.activateUsers(userIds);
        assertEquals(2, updatedRows);
        
        flushAndClear();
        
        // 验证激活结果
        List<User> activeUsers = userRepository.findByActiveTrue();
        assertEquals(2, activeUsers.size());
    }
    
    @Test
    void testUniqueConstraints() {
        // 创建第一个用户
        User user1 = createTestUser("user1", "testuser", "test@example.com");
        userRepository.save(user1);
        flushAndClear();
        
        // 尝试创建用户名重复的用户
        User user2 = createTestUser("user2", "testuser", "different@example.com");
        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
            entityManager.flush();
        });
        
        // 尝试创建邮箱重复的用户
        User user3 = createTestUser("user3", "different", "test@example.com");
        assertThrows(Exception.class, () -> {
            userRepository.save(user3);
            entityManager.flush();
        });
    }
    
    @Test
    void testDeleteUser() {
        // 创建测试用户
        User user = createTestUser("user123", "testuser", "test@example.com");
        userRepository.save(user);
        flushAndClear();
        
        // 验证用户存在
        assertTrue(userRepository.existsById("user123"));
        
        // 删除用户
        userRepository.deleteById("user123");
        flushAndClear();
        
        // 验证用户已删除
        assertFalse(userRepository.existsById("user123"));
    }
    
    /**
     * 创建测试用户的辅助方法
     */
    private User createTestUser(String userId, String username, String email) {
        User user = new User(userId, username, email);
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}