package com.education.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA配置类
 * 启用JPA审计功能和事务管理
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.education.ai.repository")
@EnableTransactionManagement
public class JpaConfig {
    
    // JPA审计功能会自动处理@CreatedDate和@LastModifiedDate注解
    // 无需额外配置，Spring Boot会自动配置AuditorAware
}