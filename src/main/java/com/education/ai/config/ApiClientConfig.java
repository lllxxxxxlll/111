package com.education.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 外部API客户端配置类
 * 
 * 配置RestTemplate和HTTP客户端
 * 设置超时时间和连接池参数
 */
@Configuration
public class ApiClientConfig {

    /**
     * 配置RestTemplate用于外部API调用
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * 配置多模态API专用的RestTemplate
     */
    @Bean("multimodalRestTemplate")
    public RestTemplate multimodalRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * 配置图像处理API专用的RestTemplate
     */
    @Bean("imageProcessingRestTemplate")
    public RestTemplate imageProcessingRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(20))
            .build();
    }

    /**
     * 配置题目解析API专用的RestTemplate
     */
    @Bean("problemSolvingRestTemplate")
    public RestTemplate problemSolvingRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(12))
            .setReadTimeout(Duration.ofSeconds(25))
            .build();
    }

    /**
     * 配置教育合作伙伴API专用的RestTemplate
     */
    @Bean("educationPartnerRestTemplate")
    public RestTemplate educationPartnerRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(8))
            .setReadTimeout(Duration.ofSeconds(15))
            .build();
    }

    /**
     * 配置ObjectMapper用于JSON序列化和反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}