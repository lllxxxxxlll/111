package com.education.ai.service;

import com.education.ai.client.EducationPartnerApiClient;
import com.education.ai.client.ImageProcessingApiClient;
import com.education.ai.client.MultimodalApiClient;
import com.education.ai.client.ProblemSolvingApiClient;
import com.education.ai.model.ApiCallLog;
import com.education.ai.repository.ApiCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * API集成管理服务
 * 
 * 统一管理所有外部API调用，提供：
 * - 统一的API调用接口
 * - 熔断器和重试机制管理
 * - API调用日志记录
 * - API健康检查和监控
 * - 性能指标收集
 */
@Service
public class ApiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ApiIntegrationService.class);
    
    private final MultimodalApiClient multimodalApiClient;
    private final ImageProcessingApiClient imageProcessingApiClient;
    private final ProblemSolvingApiClient problemSolvingApiClient;
    private final EducationPartnerApiClient educationPartnerApiClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;
    
    // API性能监控阈值配置
    @Value("${app.performance.api-response-threshold:5000}")
    private long responseTimeThreshold;
    
    @Value("${app.performance.failure-rate-threshold:0.05}")
    private double failureRateThreshold;
    
    // API健康状态缓存
    private final Map<String, ApiHealthStatus> healthStatusCache = new ConcurrentHashMap<>();
    
    // API性能指标缓存
    private final Map<String, ApiMetrics> metricsCache = new ConcurrentHashMap<>();
    
    public ApiIntegrationService(MultimodalApiClient multimodalApiClient,
                               ImageProcessingApiClient imageProcessingApiClient,
                               ProblemSolvingApiClient problemSolvingApiClient,
                               EducationPartnerApiClient educationPartnerApiClient,
                               CircuitBreakerRegistry circuitBreakerRegistry,
                               ApiCallLogRepository apiCallLogRepository,
                               ObjectMapper objectMapper) {
        this.multimodalApiClient = multimodalApiClient;
        this.imageProcessingApiClient = imageProcessingApiClient;
        this.problemSolvingApiClient = problemSolvingApiClient;
        this.educationPartnerApiClient = educationPartnerApiClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
        
        // 初始化API健康状态
        initializeHealthStatus();
    }
    
    /**
     * 统一的外部API调用方法
     * 
     * @param apiName API名称
     * @param operation API操作
     * @param responseType 响应类型
     * @return API调用结果
     */
    public <T> ApiCallResult<T> callExternalApi(String apiName, Supplier<T> operation, Class<T> responseType) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        
        try {
            logger.info("开始调用外部API: {} (请求ID: {})", apiName, requestId);
            
            // 检查API健康状态
            ApiHealthStatus healthStatus = getApiHealthStatus(apiName);
            if (healthStatus.getStatus() == HealthStatus.DOWN) {
                logger.warn("API {} 当前不可用，跳过调用", apiName);
                return ApiCallResult.failure("API服务当前不可用", null);
            }
            
            // 执行API调用
            T result = operation.get();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录成功调用
            logApiCall(apiName, requestId, null, result, 200, responseTime, true);
            updateApiMetrics(apiName, responseTime, true);
            updateHealthStatus(apiName, true, responseTime);
            
            // 检查响应时间是否超过阈值
            if (responseTime > responseTimeThreshold) {
                logger.warn("API {} 响应时间过长: {}ms (阈值: {}ms)", apiName, responseTime, responseTimeThreshold);
                triggerPerformanceAlert(apiName, responseTime);
            }
            
            logger.info("API调用成功: {} (请求ID: {}, 响应时间: {}ms)", apiName, requestId, responseTime);
            return ApiCallResult.success(result, responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("API调用失败: {} (请求ID: {}, 错误: {})", apiName, requestId, e.getMessage(), e);
            
            // 记录失败调用
            logApiCall(apiName, requestId, null, e.getMessage(), 500, responseTime, false);
            updateApiMetrics(apiName, responseTime, false);
            updateHealthStatus(apiName, false, responseTime);
            
            // 检查失败率是否超过阈值
            checkFailureRateThreshold(apiName);
            
            return ApiCallResult.failure(e.getMessage(), e);
        }
    }
    
    /**
     * 异步调用外部API
     */
    public <T> CompletableFuture<ApiCallResult<T>> callExternalApiAsync(String apiName, Supplier<T> operation, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> callExternalApi(apiName, operation, responseType));
    }
    
    /**
     * 获取API健康状态
     */
    public ApiHealthStatus getApiHealthStatus(String apiName) {
        return healthStatusCache.getOrDefault(apiName, new ApiHealthStatus(apiName, HealthStatus.UNKNOWN, "未知状态"));
    }
    
    /**
     * 获取所有API的健康状态
     */
    public Map<String, ApiHealthStatus> getAllApiHealthStatus() {
        return new HashMap<>(healthStatusCache);
    }
    
    /**
     * 获取API性能指标
     */
    public ApiMetrics getApiMetrics(String apiName) {
        return metricsCache.getOrDefault(apiName, new ApiMetrics(apiName));
    }
    
    /**
     * 获取所有API的性能指标
     */
    public Map<String, ApiMetrics> getAllApiMetrics() {
        return new HashMap<>(metricsCache);
    }
    
    /**
     * 手动触发API健康检查
     */
    public void performHealthCheck(String apiName) {
        logger.info("执行API健康检查: {}", apiName);
        
        try {
            boolean isHealthy = false;
            long responseTime = 0;
            
            // 根据API名称执行相应的健康检查
            switch (apiName) {
                case "multimodal-api":
                    isHealthy = checkMultimodalApiHealth();
                    break;
                case "image-recognition-api":
                    isHealthy = checkImageProcessingApiHealth();
                    break;
                case "problem-solving-api":
                    isHealthy = checkProblemSolvingApiHealth();
                    break;
                case "education-partner-api":
                    isHealthy = checkEducationPartnerApiHealth();
                    break;
                default:
                    logger.warn("未知的API名称: {}", apiName);
                    return;
            }
            
            updateHealthStatus(apiName, isHealthy, responseTime);
            logger.info("API健康检查完成: {} (状态: {})", apiName, isHealthy ? "健康" : "不健康");
            
        } catch (Exception e) {
            logger.error("API健康检查失败: {} (错误: {})", apiName, e.getMessage(), e);
            updateHealthStatus(apiName, false, 0);
        }
    }
    
    /**
     * 获取熔断器状态
     */
    public CircuitBreakerStatus getCircuitBreakerStatus(String apiName) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(apiName);
            return new CircuitBreakerStatus(
                apiName,
                circuitBreaker.getState().toString(),
                circuitBreaker.getMetrics().getFailureRate(),
                circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
                circuitBreaker.getMetrics().getNumberOfFailedCalls()
            );
        } catch (Exception e) {
            logger.warn("获取熔断器状态失败: {} (错误: {})", apiName, e.getMessage());
            return new CircuitBreakerStatus(apiName, "UNKNOWN", 0.0f, 0, 0);
        }
    }
    
    /**
     * 获取所有熔断器状态
     */
    public Map<String, CircuitBreakerStatus> getAllCircuitBreakerStatus() {
        Map<String, CircuitBreakerStatus> statusMap = new HashMap<>();
        String[] apiNames = {"multimodal-api", "image-recognition-api", "problem-solving-api", "education-partner-api"};
        
        for (String apiName : apiNames) {
            statusMap.put(apiName, getCircuitBreakerStatus(apiName));
        }
        
        return statusMap;
    }
    
    /**
     * 重置熔断器状态
     */
    public void resetCircuitBreaker(String apiName) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(apiName);
            circuitBreaker.reset();
            logger.info("熔断器已重置: {}", apiName);
        } catch (Exception e) {
            logger.error("重置熔断器失败: {} (错误: {})", apiName, e.getMessage(), e);
        }
    }
    
    /**
     * 初始化API健康状态
     */
    private void initializeHealthStatus() {
        String[] apiNames = {"multimodal-api", "image-recognition-api", "problem-solving-api", "education-partner-api"};
        
        for (String apiName : apiNames) {
            healthStatusCache.put(apiName, new ApiHealthStatus(apiName, HealthStatus.UNKNOWN, "初始化状态"));
            metricsCache.put(apiName, new ApiMetrics(apiName));
        }
    }
    
    /**
     * 更新API健康状态
     */
    private void updateHealthStatus(String apiName, boolean isHealthy, long responseTime) {
        ApiHealthStatus currentStatus = healthStatusCache.get(apiName);
        if (currentStatus == null) {
            currentStatus = new ApiHealthStatus(apiName, HealthStatus.UNKNOWN, "未知状态");
        }
        
        HealthStatus newStatus = isHealthy ? HealthStatus.UP : HealthStatus.DOWN;
        String message = isHealthy ? "服务正常" : "服务异常";
        
        ApiHealthStatus newHealthStatus = new ApiHealthStatus(apiName, newStatus, message);
        newHealthStatus.setLastCheckTime(LocalDateTime.now());
        newHealthStatus.setResponseTime(responseTime);
        
        healthStatusCache.put(apiName, newHealthStatus);
    }
    
    /**
     * 更新API性能指标
     */
    private void updateApiMetrics(String apiName, long responseTime, boolean success) {
        ApiMetrics metrics = metricsCache.computeIfAbsent(apiName, ApiMetrics::new);
        metrics.recordCall(responseTime, success);
    }
    
    /**
     * 检查失败率阈值
     */
    private void checkFailureRateThreshold(String apiName) {
        ApiMetrics metrics = metricsCache.get(apiName);
        if (metrics != null && metrics.getFailureRate() > failureRateThreshold) {
            logger.warn("API {} 失败率超过阈值: {:.2%} (阈值: {:.2%})", 
                       apiName, metrics.getFailureRate(), failureRateThreshold);
            triggerFailureRateAlert(apiName, metrics.getFailureRate());
        }
    }
    
    /**
     * 触发性能告警
     */
    private void triggerPerformanceAlert(String apiName, long responseTime) {
        // 这里可以集成告警系统，如发送邮件、短信或推送到监控平台
        logger.warn("性能告警: API {} 响应时间过长 {}ms", apiName, responseTime);
    }
    
    /**
     * 触发失败率告警
     */
    private void triggerFailureRateAlert(String apiName, double failureRate) {
        // 这里可以集成告警系统
        logger.warn("失败率告警: API {} 失败率过高 {:.2%}", apiName, failureRate);
    }
    
    /**
     * 记录API调用日志
     */
    private void logApiCall(String apiName, String requestId, Object requestData, 
                           Object responseData, int statusCode, long responseTime, boolean success) {
        try {
            ApiCallLog log = new ApiCallLog();
            log.setApiName(apiName + "-" + requestId);
            log.setRequestData(requestData != null ? objectMapper.writeValueAsString(requestData) : null);
            log.setResponseData(responseData != null ? objectMapper.writeValueAsString(responseData) : null);
            log.setStatusCode(statusCode);
            log.setResponseTimeMs(responseTime);
            log.setCallTime(LocalDateTime.now());
            log.setSuccess(success);
            
            apiCallLogRepository.save(log);
        } catch (Exception e) {
            logger.error("记录API调用日志失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    // 各个API的健康检查方法
    private boolean checkMultimodalApiHealth() {
        // 实现多模态API健康检查逻辑
        // 这里可以调用API的健康检查端点或发送简单的测试请求
        return true; // 简化实现
    }
    
    private boolean checkImageProcessingApiHealth() {
        // 实现图像处理API健康检查逻辑
        return true; // 简化实现
    }
    
    private boolean checkProblemSolvingApiHealth() {
        // 实现题目解析API健康检查逻辑
        return true; // 简化实现
    }
    
    private boolean checkEducationPartnerApiHealth() {
        // 实现教育合作伙伴API健康检查逻辑
        return true; // 简化实现
    }
    
    /**
     * API调用结果封装类
     */
    public static class ApiCallResult<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;
        private final Exception exception;
        private final long responseTime;
        
        private ApiCallResult(boolean success, T data, String errorMessage, Exception exception, long responseTime) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.responseTime = responseTime;
        }
        
        public static <T> ApiCallResult<T> success(T data, long responseTime) {
            return new ApiCallResult<>(true, data, null, null, responseTime);
        }
        
        public static <T> ApiCallResult<T> failure(String errorMessage, Exception exception) {
            return new ApiCallResult<>(false, null, errorMessage, exception, 0);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getErrorMessage() { return errorMessage; }
        public Exception getException() { return exception; }
        public long getResponseTime() { return responseTime; }
    }
    
    /**
     * API健康状态类
     */
    public static class ApiHealthStatus {
        private final String apiName;
        private final HealthStatus status;
        private final String message;
        private LocalDateTime lastCheckTime;
        private long responseTime;
        
        public ApiHealthStatus(String apiName, HealthStatus status, String message) {
            this.apiName = apiName;
            this.status = status;
            this.message = message;
            this.lastCheckTime = LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getApiName() { return apiName; }
        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    }
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        UP, DOWN, UNKNOWN
    }
    
    /**
     * API性能指标类
     */
    public static class ApiMetrics {
        private final String apiName;
        private long totalCalls;
        private long successfulCalls;
        private long failedCalls;
        private long totalResponseTime;
        private long maxResponseTime;
        private long minResponseTime;
        
        public ApiMetrics(String apiName) {
            this.apiName = apiName;
            this.minResponseTime = Long.MAX_VALUE;
        }
        
        public synchronized void recordCall(long responseTime, boolean success) {
            totalCalls++;
            totalResponseTime += responseTime;
            
            if (success) {
                successfulCalls++;
            } else {
                failedCalls++;
            }
            
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            minResponseTime = Math.min(minResponseTime, responseTime);
        }
        
        // Getters
        public String getApiName() { return apiName; }
        public long getTotalCalls() { return totalCalls; }
        public long getSuccessfulCalls() { return successfulCalls; }
        public long getFailedCalls() { return failedCalls; }
        public double getFailureRate() { 
            return totalCalls > 0 ? (double) failedCalls / totalCalls : 0.0; 
        }
        public double getAverageResponseTime() { 
            return totalCalls > 0 ? (double) totalResponseTime / totalCalls : 0.0; 
        }
        public long getMaxResponseTime() { return maxResponseTime; }
        public long getMinResponseTime() { return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; }
    }
    
    /**
     * 熔断器状态类
     */
    public static class CircuitBreakerStatus {
        private final String apiName;
        private final String state;
        private final float failureRate;
        private final long successfulCalls;
        private final long failedCalls;
        
        public CircuitBreakerStatus(String apiName, String state, float failureRate, 
                                  long successfulCalls, long failedCalls) {
            this.apiName = apiName;
            this.state = state;
            this.failureRate = failureRate;
            this.successfulCalls = successfulCalls;
            this.failedCalls = failedCalls;
        }
        
        // Getters
        public String getApiName() { return apiName; }
        public String getState() { return state; }
        public float getFailureRate() { return failureRate; }
        public long getSuccessfulCalls() { return successfulCalls; }
        public long getFailedCalls() { return failedCalls; }
    }
}