package com.education.ai.service;

import com.education.ai.model.ApiCallLog;
import com.education.ai.repository.ApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API指标服务
 * 
 * 提供API调用统计和性能分析功能
 */
@Service
public class ApiMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(ApiMetricsService.class);
    
    private final ApiCallLogRepository apiCallLogRepository;
    
    public ApiMetricsService(ApiCallLogRepository apiCallLogRepository) {
        this.apiCallLogRepository = apiCallLogRepository;
    }
    
    /**
     * 获取API调用统计信息
     * 
     * @param apiName API名称
     * @param hours 统计时间范围（小时）
     * @return 统计信息
     */
    public ApiStatistics getApiStatistics(String apiName, int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<ApiCallLog> logs = apiCallLogRepository.findByApiNameContainingAndCallTimeAfter(apiName, startTime);
        
        return calculateStatistics(apiName, logs);
    }
    
    /**
     * 获取所有API的统计信息
     * 
     * @param hours 统计时间范围（小时）
     * @return 所有API的统计信息
     */
    public Map<String, ApiStatistics> getAllApiStatistics(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<ApiCallLog> allLogs = apiCallLogRepository.findByCallTimeAfter(startTime);
        
        Map<String, List<ApiCallLog>> logsByApi = new HashMap<>();
        
        // 按API名称分组
        for (ApiCallLog log : allLogs) {
            String apiName = extractApiName(log.getApiName());
            logsByApi.computeIfAbsent(apiName, k -> new java.util.ArrayList<>()).add(log);
        }
        
        Map<String, ApiStatistics> statisticsMap = new HashMap<>();
        for (Map.Entry<String, List<ApiCallLog>> entry : logsByApi.entrySet()) {
            statisticsMap.put(entry.getKey(), calculateStatistics(entry.getKey(), entry.getValue()));
        }
        
        return statisticsMap;
    }
    
    /**
     * 计算API统计信息
     */
    private ApiStatistics calculateStatistics(String apiName, List<ApiCallLog> logs) {
        if (logs.isEmpty()) {
            return new ApiStatistics(apiName, 0, 0, 0, 0.0, 0.0, 0, 0);
        }
        
        long totalCalls = logs.size();
        long successfulCalls = logs.stream().mapToLong(log -> log.getSuccess() ? 1 : 0).sum();
        long failedCalls = totalCalls - successfulCalls;
        double failureRate = (double) failedCalls / totalCalls;
        
        double averageResponseTime = logs.stream()
            .mapToLong(ApiCallLog::getResponseTimeMs)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = logs.stream()
            .mapToLong(ApiCallLog::getResponseTimeMs)
            .max()
            .orElse(0);
        
        long minResponseTime = logs.stream()
            .mapToLong(ApiCallLog::getResponseTimeMs)
            .min()
            .orElse(0);
        
        return new ApiStatistics(apiName, totalCalls, successfulCalls, failedCalls, 
                               failureRate, averageResponseTime, maxResponseTime, minResponseTime);
    }
    
    /**
     * 从API调用日志名称中提取API名称
     */
    private String extractApiName(String logApiName) {
        // 移除请求ID后缀，例如 "multimodal-api-12345678" -> "multimodal-api"
        int lastDashIndex = logApiName.lastIndexOf('-');
        if (lastDashIndex > 0 && lastDashIndex < logApiName.length() - 1) {
            String suffix = logApiName.substring(lastDashIndex + 1);
            // 如果后缀是8位字符（请求ID），则移除
            if (suffix.length() == 8 && suffix.matches("[a-f0-9]+")) {
                return logApiName.substring(0, lastDashIndex);
            }
        }
        return logApiName;
    }
    
    /**
     * API统计信息类
     */
    public static class ApiStatistics {
        private final String apiName;
        private final long totalCalls;
        private final long successfulCalls;
        private final long failedCalls;
        private final double failureRate;
        private final double averageResponseTime;
        private final long maxResponseTime;
        private final long minResponseTime;
        
        public ApiStatistics(String apiName, long totalCalls, long successfulCalls, long failedCalls,
                           double failureRate, double averageResponseTime, long maxResponseTime, long minResponseTime) {
            this.apiName = apiName;
            this.totalCalls = totalCalls;
            this.successfulCalls = successfulCalls;
            this.failedCalls = failedCalls;
            this.failureRate = failureRate;
            this.averageResponseTime = averageResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.minResponseTime = minResponseTime;
        }
        
        // Getters
        public String getApiName() { return apiName; }
        public long getTotalCalls() { return totalCalls; }
        public long getSuccessfulCalls() { return successfulCalls; }
        public long getFailedCalls() { return failedCalls; }
        public double getFailureRate() { return failureRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public long getMinResponseTime() { return minResponseTime; }
        
        @Override
        public String toString() {
            return String.format("ApiStatistics{apiName='%s', totalCalls=%d, successfulCalls=%d, failedCalls=%d, " +
                               "failureRate=%.2f%%, averageResponseTime=%.2fms, maxResponseTime=%dms, minResponseTime=%dms}",
                               apiName, totalCalls, successfulCalls, failedCalls, failureRate * 100,
                               averageResponseTime, maxResponseTime, minResponseTime);
        }
    }
}