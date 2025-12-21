package com.education.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI教育辅助应用后端系统主应用类
 * 
 * 提供三大核心功能：
 * 1. 手势识别内容分析
 * 2. 拍照搜题可视化演示
 * 3. 实时检测实验辅助
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class AiEducationAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEducationAssistantApplication.class, args);
    }
}