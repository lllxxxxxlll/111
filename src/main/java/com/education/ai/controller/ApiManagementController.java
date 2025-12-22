package com.education.ai.controller;

import com.education.ai.service.ApiIntegrationService;
import com.education.ai.service.ApiMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API管理控制器
 * 
 * 提供API集成管理和监控的REST接口
 */
@RestController
@RequestMapping("/api/v1/management")
public class ApiManagementController {

    private final ApiIntegrationService apiIntegrationService;
    private final ApiMetricsService apiMetricsService;
    
    public ApiManagementController(ApiIntegrationService apiIntegrationService,
                                 ApiMetricsService apiMetricsService) {
        this.apiIntegrationService = apiIntegrationService;
        this.apiMetricsService = apiMetricsService;
    }
    
    /**
     * 获取所有API的健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, ApiIntegrationService.ApiHealthStatus>> getAllApiHealthStatus() {
        Map<String, ApiIntegrationService.ApiHealthStatus> healthStatus = apiIntegrationService.getAllApiHealthStatus();
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * 获取指定API的健康状态
     */
    @GetMapping("/health/{apiName}")
    public ResponseEntity<ApiIntegrationService.ApiHealthStatus> getApiHealthStatus(@PathVariable String apiName) {
        ApiIntegrationService.ApiHealthStatus healthStatus = apiIntegrationService.getApiHealthStatus(apiName);
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * 手动触发API健康检查
     */
    @PostMapping("/health/{apiName}/check")
    public ResponseEntity<String> performHealthCheck(@PathVariable String apiName) {
        apiIntegrationService.performHealthCheck(apiName);
        return ResponseEntity.ok("健康检查已触发");
    }
    
    /**
     * 获取所有API的性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, ApiIntegrationService.ApiMetrics>> getAllApiMetrics() {
        Map<String, ApiIntegrationService.ApiMetrics> metrics = apiIntegrationService.getAllApiMetrics();
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取指定API的性能指标
     */
    @GetMapping("/metrics/{apiName}")
    public ResponseEntity<ApiIntegrationService.ApiMetrics> getApiMetrics(@PathVariable String apiName) {
        ApiIntegrationService.ApiMetrics metrics = apiIntegrationService.getApiMetrics(apiName);
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取所有熔断器状态
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, ApiIntegrationService.CircuitBreakerStatus>> getAllCircuitBreakerStatus() {
        Map<String, ApiIntegrationService.CircuitBreakerStatus> status = apiIntegrationService.getAllCircuitBreakerStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取指定API的熔断器状态
     */
    @GetMapping("/circuit-breakers/{apiName}")
    public ResponseEntity<ApiIntegrationService.CircuitBreakerStatus> getCircuitBreakerStatus(@PathVariable String apiName) {
        ApiIntegrationService.CircuitBreakerStatus status = apiIntegrationService.getCircuitBreakerStatus(apiName);
        return ResponseEntity.ok(status);
    }
    
    /**
     * 重置熔断器状态
     */
    @PostMapping("/circuit-breakers/{apiName}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String apiName) {
        apiIntegrationService.resetCircuitBreaker(apiName);
        return ResponseEntity.ok("熔断器已重置");
    }
    
    /**
     * 获取API统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, ApiMetricsService.ApiStatistics>> getAllApiStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, ApiMetricsService.ApiStatistics> statistics = apiMetricsService.getAllApiStatistics(hours);
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 获取指定API的统计信息
     */
    @GetMapping("/statistics/{apiName}")
    public ResponseEntity<ApiMetricsService.ApiStatistics> getApiStatistics(
            @PathVariable String apiName,
            @RequestParam(defaultValue = "24") int hours) {
        ApiMetricsService.ApiStatistics statistics = apiMetricsService.getApiStatistics(apiName, hours);
        return ResponseEntity.ok(statistics);
    }
}