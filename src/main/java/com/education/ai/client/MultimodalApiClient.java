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
 * 多模态大模型API客户端
 * 
 * 负责调用外部多模态AI API进行手势识别和内容分析
 * 支持熔断器和重试机制
 */
@Component
public class MultimodalApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalApiClient.class);
    
    private final RestTemplate restTemplate;
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${external-api.multimodal.base-url}")
    private String baseUrl;
    
    @Value("${external-api.multimodal.api-key}")
    private String apiKey;
    
    public MultimodalApiClient(@Qualifier("multimodalRestTemplate") RestTemplate restTemplate,
                              ApiCallLogRepository apiCallLogRepository,
                              ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 分析手势指向的内容
     * 
     * @param gestureData 手势数据
     * @param contentImage 内容图像（base64编码）
     * @param userId 用户ID
     * @return 分析结果
     */
    @CircuitBreaker(name = "multimodal-api", fallbackMethod = "analyzeGestureContentFallback")
    @Retry(name = "default")
    public MultimodalAnalysisResponse analyzeGestureContent(String gestureData, String contentImage, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/analyze/gesture";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("gestureData", gestureData);
            requestBody.put("contentImage", contentImage);
            requestBody.put("userId", userId);
            requestBody.put("analysisType", "gesture_content");
            
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
            logApiCall("multimodal-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseAnalysisResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("多模态API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("multimodal-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("多模态API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 熔断器降级方法
     */
    public MultimodalAnalysisResponse analyzeGestureContentFallback(String gestureData, String contentImage, 
                                                                   String userId, Exception ex) {
        logger.warn("多模态API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        MultimodalAnalysisResponse fallbackResponse = new MultimodalAnalysisResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("服务暂时不可用，请稍后重试");
        fallbackResponse.setConfidence(0.0);
        
        return fallbackResponse;
    }
    
    /**
     * 解析API响应
     */
    private MultimodalAnalysisResponse parseAnalysisResponse(JsonNode responseJson) {
        MultimodalAnalysisResponse response = new MultimodalAnalysisResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setAnalysisId(responseJson.path("analysisId").asText());
            response.setExplanation(responseJson.path("explanation").asText());
            response.setConfidence(responseJson.path("confidence").asDouble());
            
            // 解析关键点
            if (responseJson.has("keyPoints") && responseJson.get("keyPoints").isArray()) {
                String[] keyPoints = objectMapper.convertValue(
                    responseJson.get("keyPoints"), String[].class);
                response.setKeyPoints(keyPoints);
            }
            
            response.setProcessingTime(responseJson.path("processingTime").asLong());
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
     * 多模态分析响应类
     */
    public static class MultimodalAnalysisResponse {
        private boolean success;
        private String analysisId;
        private String explanation;
        private String[] keyPoints;
        private Double confidence;
        private Long processingTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getAnalysisId() { return analysisId; }
        public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public String[] getKeyPoints() { return keyPoints; }
        public void setKeyPoints(String[] keyPoints) { this.keyPoints = keyPoints; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}