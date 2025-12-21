package com.education.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 主应用类测试
 */
@SpringBootTest
@ActiveProfiles("test")
class AiEducationAssistantApplicationTests {

    @Test
    void contextLoads() {
        // 测试Spring Boot应用上下文是否能正常加载
    }
}