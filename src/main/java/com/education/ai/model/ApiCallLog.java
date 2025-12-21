package com.education.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * API调用日志实体类
 * 记录所有外部API调用的详细信息，用于监控和审计
 */
@Entity
@Table(name = "api_call_logs", indexes = {
    @Index(name = "idx_api_name", columnList = "api_name"),
    @Index(name = "idx_api_call_time", columnList = "call_time"),
    @Index(name = "idx_api_success", columnList = "success"),
    @Index(name = "idx_api_status_code", columnList = "status_code"),
    @Index(name = "idx_api_response_time", columnList = "response_time_ms")
})
@EntityListeners(AuditingEntityListener.class)
public class ApiCallLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @NotBlank(message = "API名称不能为空")
    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;
    
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @NotNull(message = "响应时间不能为空")
    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;
    
    @CreatedDate
    @Column(name = "call_time", nullable = false, updatable = false)
    private LocalDateTime callTime;
    
    @NotNull(message = "成功标识不能为空")
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "user_id", length = 50)
    private String userId;
    
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    @Column(name = "endpoint", length = 500)
    private String endpoint;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "request_size_bytes")
    private Long requestSizeBytes;
    
    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    // 构造函数
    public ApiCallLog() {}
    
    public ApiCallLog(String apiName, Long responseTimeMs, Boolean success) {
        this.apiName = apiName;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
    }
    
    // Getters and Setters
    public Long getLogId() {
        return logId;
    }
    
    public void setLogId(Long logId) {
        this.logId = logId;
    }
    
    public String getApiName() {
        return apiName;
    }
    
    public void setApiName(String apiName) {
        this.apiName = apiName;
    }
    
    public String getRequestData() {
        return requestData;
    }
    
    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }
    
    public String getResponseData() {
        return responseData;
    }
    
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public LocalDateTime getCallTime() {
        return callTime;
    }
    
    public void setCallTime(LocalDateTime callTime) {
        this.callTime = callTime;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public Long getRequestSizeBytes() {
        return requestSizeBytes;
    }
    
    public void setRequestSizeBytes(Long requestSizeBytes) {
        this.requestSizeBytes = requestSizeBytes;
    }
    
    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }
    
    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    /**
     * 检查API调用是否成功且响应时间合理
     */
    public boolean isHealthy() {
        return success && responseTimeMs != null && responseTimeMs < 5000; // 5秒阈值
    }
    
    /**
     * 检查是否为慢查询
     */
    public boolean isSlowQuery() {
        return responseTimeMs != null && responseTimeMs > 3000; // 3秒阈值
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiCallLog that = (ApiCallLog) o;
        return Objects.equals(logId, that.logId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }
    
    @Override
    public String toString() {
        return "ApiCallLog{" +
                "logId=" + logId +
                ", apiName='" + apiName + '\'' +
                ", statusCode=" + statusCode +
                ", responseTimeMs=" + responseTimeMs +
                ", success=" + success +
                ", callTime=" + callTime +
                '}';
    }
}