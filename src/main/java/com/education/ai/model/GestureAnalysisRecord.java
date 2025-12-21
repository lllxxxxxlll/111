package com.education.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 手势分析记录实体类
 * 存储手势识别和内容分析的结果数据
 */
@Entity
@Table(name = "gesture_analysis_records", indexes = {
    @Index(name = "idx_gesture_user_id", columnList = "user_id"),
    @Index(name = "idx_gesture_created_at", columnList = "created_at"),
    @Index(name = "idx_gesture_confidence", columnList = "confidence")
})
@EntityListeners(AuditingEntityListener.class)
public class GestureAnalysisRecord {
    
    @Id
    @Column(name = "analysis_id", length = 50)
    private String analysisId;
    
    @NotBlank(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @NotBlank(message = "手势数据不能为空")
    @Column(name = "gesture_data", nullable = false, columnDefinition = "TEXT")
    private String gestureData;
    
    @Column(name = "content_image", columnDefinition = "LONGTEXT")
    private String contentImage;
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    @DecimalMin(value = "0.0", message = "置信度不能小于0")
    @DecimalMax(value = "1.0", message = "置信度不能大于1")
    @Column(name = "confidence")
    private Double confidence;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @NotNull(message = "处理时间不能为空")
    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status = AnalysisStatus.PENDING;
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    @Column(name = "api_response", columnDefinition = "TEXT")
    private String apiResponse;
    
    @Column(name = "subject_area", length = 50)
    private String subjectArea;
    
    // 构造函数
    public GestureAnalysisRecord() {}
    
    public GestureAnalysisRecord(String analysisId, String userId, String gestureData) {
        this.analysisId = analysisId;
        this.userId = userId;
        this.gestureData = gestureData;
    }
    
    // Getters and Setters
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getGestureData() {
        return gestureData;
    }
    
    public void setGestureData(String gestureData) {
        this.gestureData = gestureData;
    }
    
    public String getContentImage() {
        return contentImage;
    }
    
    public void setContentImage(String contentImage) {
        this.contentImage = contentImage;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public AnalysisStatus getStatus() {
        return status;
    }
    
    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getApiResponse() {
        return apiResponse;
    }
    
    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
    }
    
    public String getSubjectArea() {
        return subjectArea;
    }
    
    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GestureAnalysisRecord that = (GestureAnalysisRecord) o;
        return Objects.equals(analysisId, that.analysisId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(analysisId);
    }
    
    @Override
    public String toString() {//序列化
        return "GestureAnalysisRecord{" +
                "analysisId='" + analysisId + '\'' +
                ", userId='" + userId + '\'' +
                ", confidence=" + confidence +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}