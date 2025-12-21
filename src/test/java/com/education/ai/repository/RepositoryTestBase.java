package com.education.ai.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Repository测试基类
 * 提供通用的测试配置和工具方法
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class RepositoryTestBase {
    
    @Autowired
    protected TestEntityManager entityManager;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        entityManager.clear();
    }
    
    /**
     * 刷新并清理持久化上下文
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}