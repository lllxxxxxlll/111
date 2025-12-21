package com.education.ai.client;

import com.education.ai.model.ApiCallLog;
import com.education.ai.repository.ApiCallLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 教育合作伙伴API客户端
 * 
 * 负责调用教育合作伙伴API进行实验检测和教育资源集成
 * 支持熔断器和重试机制
 */
@Component
public class EducationPartnerApiClient {

    private static final Logger logger = LoggerFactory.getLogger(EducationPartnerApiClient.class);
    
    private final RestTemplate restTemplate;
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${external-api.education-partner.base-url}")
    private String baseUrl;
    
    @Value("${external-api.education-partner.api-key}")
    private String apiKey;
    
    public EducationPartnerApiClient(@Qualifier("educationPartnerRestTemplate") RestTemplate restTemplate,
                                   ApiCallLogRepository apiCallLogRepository,
                                   ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 启动实验检测会话
     * 
     * @param sessionId 会话ID
     * @param experimentType 实验类型
     * @param userId 用户ID
     * @return 会话启动结果
     */
    @CircuitBreaker(name = "education-partner-api", fallbackMethod = "startExperimentSessionFallback")
    @Retry(name = "default")
    public ExperimentSessionResponse startExperimentSession(String sessionId, String experimentType, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/experiment/start";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionId);
            requestBody.put("experimentType", experimentType);
            requestBody.put("userId", userId);
            requestBody.put("timestamp", LocalDateTime.now().toString());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-API-Version", "v1");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录API调用日志
            logApiCall("education-partner-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseSessionResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("教育合作伙伴API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("education-partner-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("教育合作伙伴API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送实验数据进行分析
     * 
     * @param sessionId 会话ID
     * @param experimentData 实验数据
     * @param userId 用户ID
     * @return 分析结果
     */
    @CircuitBreaker(name = "education-partner-api", fallbackMethod = "analyzeExperimentDataFallback")
    @Retry(name = "default")
    public ExperimentAnalysisResponse analyzeExperimentData(String sessionId, Map<String, Object> experimentData, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/experiment/analyze";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionId);
            requestBody.put("experimentData", experimentData);
            requestBody.put("userId", userId);
            requestBody.put("timestamp", LocalDateTime.now().toString());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-API-Version", "v1");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录API调用日志
            logApiCall("education-partner-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseAnalysisResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("实验数据分析API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("education-partner-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("实验数据分析API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 结束实验会话并生成报告
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 实验报告
     */
    @CircuitBreaker(name = "education-partner-api", fallbackMethod = "endExperimentSessionFallback")
    @Retry(name = "default")
    public ExperimentReportResponse endExperimentSession(String sessionId, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/experiment/end";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionId);
            requestBody.put("userId", userId);
            requestBody.put("endTime", LocalDateTime.now().toString());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-API-Version", "v1");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录API调用日志
            logApiCall("education-partner-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseReportResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("结束实验会话API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("education-partner-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("结束实验会话API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动实验会话熔断器降级方法
     */
    public ExperimentSessionResponse startExperimentSessionFallback(String sessionId, String experimentType, 
                                                                   String userId, Exception ex) {
        logger.warn("教育合作伙伴API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ExperimentSessionResponse fallbackResponse = new ExperimentSessionResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("实验检测服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 实验数据分析熔断器降级方法
     */
    public ExperimentAnalysisResponse analyzeExperimentDataFallback(String sessionId, Map<String, Object> experimentData, 
                                                                   String userId, Exception ex) {
        logger.warn("实验数据分析API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ExperimentAnalysisResponse fallbackResponse = new ExperimentAnalysisResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("实验数据分析服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 结束实验会话熔断器降级方法
     */
    public ExperimentReportResponse endExperimentSessionFallback(String sessionId, String userId, Exception ex) {
        logger.warn("结束实验会话API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ExperimentReportResponse fallbackResponse = new ExperimentReportResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("实验报告生成服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 解析会话响应
     */
    private ExperimentSessionResponse parseSessionResponse(JsonNode responseJson) {
        ExperimentSessionResponse response = new ExperimentSessionResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setSessionId(responseJson.path("sessionId").asText());
            response.setStatus(responseJson.path("status").asText());
            response.setWebSocketUrl(responseJson.path("webSocketUrl").asText());
            response.setExperimentConfig(responseJson.path("experimentConfig").toString());
        } else {
            response.setSuccess(false);
            response.setErrorMessage(responseJson.path("error").asText("未知错误"));
        }
        
        return response;
    }
    
    /**
     * 解析分析响应
     */
    private ExperimentAnalysisResponse parseAnalysisResponse(JsonNode responseJson) {
        ExperimentAnalysisResponse response = new ExperimentAnalysisResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setAnalysisId(responseJson.path("analysisId").asText());
            response.setStatus(responseJson.path("status").asText());
            response.setAnomalies(responseJson.path("anomalies").toString());
            response.setRecommendations(responseJson.path("recommendations").toString());
            response.setProcessingTime(responseJson.path("processingTime").asLong());
        } else {
            response.setSuccess(false);
            response.setErrorMessage(responseJson.path("error").asText("未知错误"));
        }
        
        return response;
    }
    
    /**
     * 解析报告响应
     */
    private ExperimentReportResponse parseReportResponse(JsonNode responseJson) {
        ExperimentReportResponse response = new ExperimentReportResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setReportId(responseJson.path("reportId").asText());
            response.setReportData(responseJson.path("reportData").toString());
            response.setSummary(responseJson.path("summary").asText());
            response.setGenerationTime(responseJson.path("generationTime").asLong());
        } else {
            response.setSuccess(false);
            response.setErrorMessage(responseJson.path("error").asText("未知错误"));
        }
        
        return response;
    }
    
    /**
     * 记录API调用日志
     */
    private void logApiCall(String apiName, String endpoint, Object requestData, 
                           String responseData, int statusCode, long responseTime, boolean success) {
        try {
            ApiCallLog log = new ApiCallLog();
            log.setApiName(apiName + endpoint);
            log.setRequestData(requestData != null ? objectMapper.writeValueAsString(requestData) : null);
            log.setResponseData(responseData);
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
     * 实验会话响应类
     */
    public static class ExperimentSessionResponse {
        private boolean success;
        private String sessionId;
        private String status;
        private String webSocketUrl;
        private String experimentConfig;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getWebSocketUrl() { return webSocketUrl; }
        public void setWebSocketUrl(String webSocketUrl) { this.webSocketUrl = webSocketUrl; }
        
        public String getExperimentConfig() { return experimentConfig; }
        public void setExperimentConfig(String experimentConfig) { this.experimentConfig = experimentConfig; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 实验分析响应类
     */
    public static class ExperimentAnalysisResponse {
        private boolean success;
        private String analysisId;
        private String status;
        private String anomalies;
        private String recommendations;
        private Long processingTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getAnalysisId() { return analysisId; }
        public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getAnomalies() { return anomalies; }
        public void setAnomalies(String anomalies) { this.anomalies = anomalies; }
        
        public String getRecommendations() { return recommendations; }
        public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 实验报告响应类
     */
    public static class ExperimentReportResponse {
        private boolean success;
        private String reportId;
        private String reportData;
        private String summary;
        private Long generationTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        
        public String getReportData() { return reportData; }
        public void setReportData(String reportData) { this.reportData = reportData; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public Long getGenerationTime() { return generationTime; }
        public void setGenerationTime(Long generationTime) { this.generationTime = generationTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}