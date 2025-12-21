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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 图像识别API客户端
 * 
 * 负责调用外部图像识别API进行OCR和图像内容识别
 * 支持熔断器和重试机制
 */
@Component
public class ImageProcessingApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingApiClient.class);
    
    private final RestTemplate restTemplate;
    private final ApiCallLogRepository apiCallLogRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${external-api.image-processing.base-url}")
    private String baseUrl;
    
    @Value("${external-api.image-processing.api-key}")
    private String apiKey;
    
    public ImageProcessingApiClient(@Qualifier("imageProcessingRestTemplate") RestTemplate restTemplate,
                                  ApiCallLogRepository apiCallLogRepository,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.apiCallLogRepository = apiCallLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 识别图像中的题目内容
     * 
     * @param imageFile 图像文件
     * @param userId 用户ID
     * @return 识别结果
     */
    @CircuitBreaker(name = "image-recognition-api", fallbackMethod = "recognizeProblemImageFallback")
    @Retry(name = "default")
    public ImageRecognitionResponse recognizeProblemImage(MultipartFile imageFile, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/ocr/problem";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 验证图像格式
            if (!isValidImageFormat(imageFile)) {
                throw new IllegalArgumentException("不支持的图像格式");
            }
            
            // 将图像转换为base64
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);
            requestBody.put("imageFormat", getImageFormat(imageFile));
            requestBody.put("userId", userId);
            requestBody.put("recognitionType", "problem_text");
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
            logApiCall("image-recognition-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseRecognitionResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("图像识别API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("image-recognition-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("图像识别API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 识别通用图像内容
     * 
     * @param imageFile 图像文件
     * @param userId 用户ID
     * @return 识别结果
     */
    @CircuitBreaker(name = "image-recognition-api", fallbackMethod = "recognizeGeneralImageFallback")
    @Retry(name = "default")
    public ImageRecognitionResponse recognizeGeneralImage(MultipartFile imageFile, String userId) {
        long startTime = System.currentTimeMillis();
        String endpoint = "/ocr/general";
        String fullUrl = baseUrl + endpoint;
        
        try {
            // 验证图像格式
            if (!isValidImageFormat(imageFile)) {
                throw new IllegalArgumentException("不支持的图像格式");
            }
            
            // 将图像转换为base64
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);
            requestBody.put("imageFormat", getImageFormat(imageFile));
            requestBody.put("userId", userId);
            requestBody.put("recognitionType", "general_text");
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
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
            logApiCall("image-recognition-api", endpoint, requestBody, response.getBody(), 
                      response.getStatusCodeValue(), responseTime, true);
            
            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return parseRecognitionResponse(responseJson);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("图像识别API调用失败: {}", e.getMessage(), e);
            
            // 记录失败日志
            logApiCall("image-recognition-api", endpoint, null, e.getMessage(), 
                      500, responseTime, false);
            
            throw new RestClientException("图像识别API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 题目图像识别熔断器降级方法
     */
    public ImageRecognitionResponse recognizeProblemImageFallback(MultipartFile imageFile, String userId, Exception ex) {
        logger.warn("图像识别API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ImageRecognitionResponse fallbackResponse = new ImageRecognitionResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("图像识别服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 通用图像识别熔断器降级方法
     */
    public ImageRecognitionResponse recognizeGeneralImageFallback(MultipartFile imageFile, String userId, Exception ex) {
        logger.warn("图像识别API熔断器激活，使用降级处理: {}", ex.getMessage());
        
        ImageRecognitionResponse fallbackResponse = new ImageRecognitionResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setErrorMessage("图像识别服务暂时不可用，请稍后重试");
        
        return fallbackResponse;
    }
    
    /**
     * 验证图像格式
     */
    private boolean isValidImageFormat(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/webp") ||
            contentType.equals("image/jpg")
        );
    }
    
    /**
     * 获取图像格式
     */
    private String getImageFormat(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (contentType != null) {
            return contentType.substring(contentType.lastIndexOf("/") + 1);
        }
        return "unknown";
    }
    
    /**
     * 解析API响应
     */
    private ImageRecognitionResponse parseRecognitionResponse(JsonNode responseJson) {
        ImageRecognitionResponse response = new ImageRecognitionResponse();
        
        if (responseJson.has("success") && responseJson.get("success").asBoolean()) {
            response.setSuccess(true);
            response.setRecognitionId(responseJson.path("recognitionId").asText());
            response.setRecognizedText(responseJson.path("recognizedText").asText());
            response.setConfidence(responseJson.path("confidence").asDouble());
            
            // 解析文本区域
            if (responseJson.has("textRegions") && responseJson.get("textRegions").isArray()) {
                TextRegion[] textRegions = objectMapper.convertValue(
                    responseJson.get("textRegions"), TextRegion[].class);
                response.setTextRegions(textRegions);
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
     * 图像识别响应类
     */
    public static class ImageRecognitionResponse {
        private boolean success;
        private String recognitionId;
        private String recognizedText;
        private TextRegion[] textRegions;
        private Double confidence;
        private Long processingTime;
        private String errorMessage;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getRecognitionId() { return recognitionId; }
        public void setRecognitionId(String recognitionId) { this.recognitionId = recognitionId; }
        
        public String getRecognizedText() { return recognizedText; }
        public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
        
        public TextRegion[] getTextRegions() { return textRegions; }
        public void setTextRegions(TextRegion[] textRegions) { this.textRegions = textRegions; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 文本区域类
     */
    public static class TextRegion {
        private String text;
        private BoundingBox boundingBox;
        private Double confidence;
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
    
    /**
     * 边界框类
     */
    public static class BoundingBox {
        private int x;
        private int y;
        private int width;
        private int height;
        
        // Getters and Setters
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
}