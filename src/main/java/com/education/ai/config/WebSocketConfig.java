package com.education.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 
 * 配置WebSocket消息代理和端点
 * 支持实验检测的实时数据传输
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于向客户端发送消息
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用程序目的地前缀，用于客户端发送消息
        config.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目的地前缀，用于点对点消息
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册实验检测WebSocket端点
        registry.addEndpoint("/ws/experiment")
            .setAllowedOriginPatterns("*")
            .withSockJS();
        
        // 注册通用WebSocket端点
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}