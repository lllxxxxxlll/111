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
 * 题目解析API客户端
 * 
 * 负责调用外部题目解析API进行题目分析和解答生成
 * 支持熔断器和重试机制
 */
@Component
public class ProblemSolvingApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ProblemSolvingApiClient.class);
    
    private final RestTemplate restTemplate;
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${external-api.problem-solving.base-url}")
    private String baseUrl;
    
    @Value("${external-api.problem-solving.api-key}")
    private String apiKey;
    
    public ProblemSolvingApiClient(@Qualifier("problemSolvingRestTemplate") RestTemplate restTemplate,
                                 ApiCallLogRepository apiCallLogRepository,
                                 ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 解析题目并生成解答步骤
     * 
     * @param problemText 题目文本
     * @param subject 学科类型
     * @param userId 用户ID
     * @return 解答结果
     */
    @CircuitBreaker(name = "problem-solving-api", fallbackMethod = "solveProblemFallback")
    @Retry(name = "default")
    public ProblemSolutionResponse solveProblem(String problemText, String subject, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/solve";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("problemText", problemText);
            requestBody.put("subject", subject);
            requestBody.put("userId", userId);
            requestBody.put("solutionType", "step_by_step");
            requestBody.put("includeVisualization", true);
            requestBody.put("language", "zh-CN");
            
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
            logApiCall("problem-solving-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseSolutionResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("题目解析API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("problem-solving-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("题目解析API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成可视化演示内容
     * 
     * @param problemText 题目文本
     * @param solutionSteps 解答步骤
     * @param userId 用户ID
     * @return 可视化内容
     */
    @CircuitBreaker(name = "problem-solving-api", fallbackMethod = "generateVisualizationFallback")
    @Retry(name = "default")
    public VisualizationResponse generateVisualization(String problemText, String[] solutionSteps, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/visualize";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("problemText", problemText);
            requestBody.put("solutionSteps", solutionSteps);
            requestBody.put("userId", userId);
            requestBody.put("visualizationType", "interactive");
            requestBody.put("format", "json");
            
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
            logApiCall("problem-solving-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseVisualizationResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("可视化生成API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("problem-solving-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("可视化生成API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 题目解析熔断器降级方法
     */
    public ProblemSolutionResponse solveProblemFallback(String problemText, String subject, String userId, Exception ex) {
        logger.warn("题目解析API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ProblemSolutionResponse fallbackResponse = new ProblemSolutionResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("题目解析服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 可视化生成熔断器降级方法
     */
    public VisualizationResponse generateVisualizationFallback(String problemText, String[] solutionSteps, 
                                                              String userId, Exception ex) {
        logger.warn("可视化生成API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        VisualizationResponse fallbackResponse = new VisualizationResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("可视化生成服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 解析题目解答响应
     */
    private ProblemSolutionResponse parseSolutionResponse(JsonNode responseJson) {
        ProblemSolutionResponse response = new ProblemSolutionResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setSolutionId(responseJson.path("solutionId").asText());
            response.setProblemType(responseJson.path("problemType").asText());
            response.setDifficulty(responseJson.path("difficulty").asText());
            
            // 解析解答步骤
            if (responseJson.has("solutionSteps") && responseJson.get("solutionSteps").isArray()) {
                SolutionStep[] steps = objectMapper.convertValue(
                    responseJson.get("solutionSteps"), SolutionStep[].class);
                response.setSolutionSteps(steps);
            }
            
            // 解析知识点
            if (responseJson.has("knowledgePoints") && responseJson.get("knowledgePoints").isArray()) {
                String[] knowledgePoints = objectMapper.convertValue(
                    responseJson.get("knowledgePoints"), String[].class);
                response.setKnowledgePoints(knowledgePoints);
            }
            
            response.setProcessingTime(responseJson.path("processingTime").asLong());
        } else {
            response.setSuccess(false);
            response.setErrorMessage(responseJson.path("error").asText("未知错误"));
        }
        
        return response;
    }
    
    /**
     * 解析可视化响应
     */
    private VisualizationResponse parseVisualizationResponse(JsonNode responseJson) {
        VisualizationResponse response = new VisualizationResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setVisualizationId(responseJson.path("visualizationId").asText());
            response.setVisualizationType(responseJson.path("visualizationType").asText());
            response.setVisualizationData(responseJson.path("visualizationData").toString());
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
     * 题目解答响应类
     */
    public static class ProblemSolutionResponse {
        private boolean success;
        private String solutionId;
        private String problemType;
        private String difficulty;
        private SolutionStep[] solutionSteps;
        private String[] knowledgePoints;
        private Long processingTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getSolutionId() { return solutionId; }
        public void setSolutionId(String solutionId) { this.solutionId = solutionId; }
        
        public String getProblemType() { return problemType; }
        public void setProblemType(String problemType) { this.problemType = problemType; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        
        public SolutionStep[] getSolutionSteps() { return solutionSteps; }
        public void setSolutionSteps(SolutionStep[] solutionSteps) { this.solutionSteps = solutionSteps; }
        
        public String[] getKnowledgePoints() { return knowledgePoints; }
        public void setKnowledgePoints(String[] knowledgePoints) { this.knowledgePoints = knowledgePoints; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 解答步骤类
     */
    public static class SolutionStep {
        private int stepNumber;
        private String description;
        private String formula;
        private String explanation;
        
        // Getters and Setters
        public int getStepNumber() { return stepNumber; }
        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
    
    /**
     * 可视化响应类
     */
    public static class VisualizationResponse {
        private boolean success;
        private String visualizationId;
        private String visualizationType;
        private String visualizationData;
        private Long processingTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getVisualizationId() { return visualizationId; }
        public void setVisualizationId(String visualizationId) { this.visualizationId = visualizationId; }
        
        public String getVisualizationType() { return visualizationType; }
        public void setVisualizationType(String visualizationType) { this.visualizationType = visualizationType; }
        
        public String getVisualizationData() { return visualizationData; }
        public void setVisualizationData(String visualizationData) { this.visualizationData = visualizationData; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}