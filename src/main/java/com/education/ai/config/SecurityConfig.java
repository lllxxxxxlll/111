package com.education.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security安全配置类
 * 
 * 配置认证、授权、CORS等安全相关设置
 * 支持JWT token认证和基于角色的访问控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.security.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.security.cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${app.security.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.security.cors.allow-credentials}")
    private boolean allowCredentials;

    /**
     * 配置HTTP安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF，因为使用JWT token
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置会话管理为无状态
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权
            .authorizeHttpRequests(authz -> authz
                // 公开端点
                .requestMatchers(
                    "/auth/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // WebSocket端点
                .requestMatchers("/ws/**").permitAll()
                
                // 手势识别端点 - 需要认证
                .requestMatchers("/v1/gesture/**").authenticated()
                
                // 拍照搜题端点 - 需要认证
                .requestMatchers("/v1/photo/**").authenticated()
                
                // 实验检测端点 - 需要认证
                .requestMatchers("/v1/experiment/**").authenticated()
                
                // 管理端点 - 需要管理员权限
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 设置允许的源
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins));
        
        // 设置允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        
        // 设置允许的请求头
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        
        // 设置是否允许凭证
        configuration.setAllowCredentials(allowCredentials);
        
        // 设置预检请求的缓存时间
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}