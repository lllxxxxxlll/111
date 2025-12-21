package com.education.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 实验会话实体类
 * 存储实验检测会话的状态和配置信息
 */
@Entity
@Table(name = "experiment_sessions", indexes = {
    @Index(name = "idx_experiment_user_id", columnList = "user_id"),
    @Index(name = "idx_experiment_status", columnList = "status"),
    @Index(name = "idx_experiment_start_time", columnList = "start_time"),
    @Index(name = "idx_experiment_type", columnList = "experiment_type")
})
@EntityListeners(AuditingEntityListener.class)
public class ExperimentSession {
    
    @Id
    @Column(name = "session_id", length = 50)
    private String sessionId;
    
    @NotBlank(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @NotBlank(message = "实验类型不能为空")
    @Column(name = "experiment_type", nullable = false, length = 100)
    private String experimentType;
    
    @CreatedDate
    @Column(name = "start_time", nullable = false, updatable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.CREATED;
    
    @Column(name = "config_data", columnDefinition = "TEXT")
    private String configData;
    
    @Column(name = "report_data", columnDefinition = "LONGTEXT")
    private String reportData;
    
    @LastModifiedDate
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "experiment_name", length = 200)
    private String experimentName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "total_data_points")
    private Integer totalDataPoints = 0;
    
    @Column(name = "alert_count")
    private Integer alertCount = 0;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "websocket_connection_id", length = 100)
    private String websocketConnectionId;
    
    // 构造函数
    public ExperimentSession() {}
    
    public ExperimentSession(String sessionId, String userId, String experimentType) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.experimentType = experimentType;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getExperimentType() {
        return experimentType;
    }
    
    public void setExperimentType(String experimentType) {
        this.experimentType = experimentType;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public String getConfigData() {
        return configData;
    }
    
    public void setConfigData(String configData) {
        this.configData = configData;
    }
    
    public String getReportData() {
        return reportData;
    }
    
    public void setReportData(String reportData) {
        this.reportData = reportData;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getExperimentName() {
        return experimentName;
    }
    
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getTotalDataPoints() {
        return totalDataPoints;
    }
    
    public void setTotalDataPoints(Integer totalDataPoints) {
        this.totalDataPoints = totalDataPoints;
    }
    
    public Integer getAlertCount() {
        return alertCount;
    }
    
    public void setAlertCount(Integer alertCount) {
        this.alertCount = alertCount;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public String getWebsocketConnectionId() {
        return websocketConnectionId;
    }
    
    public void setWebsocketConnectionId(String websocketConnectionId) {
        this.websocketConnectionId = websocketConnectionId;
    }
    
    /**
     * 计算会话持续时间（分钟）
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.durationMinutes = (int) java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }
    
    /**
     * 增加数据点计数
     */
    public void incrementDataPoints() {
        this.totalDataPoints = (this.totalDataPoints == null ? 0 : this.totalDataPoints) + 1;
    }
    
    /**
     * 增加警告计数
     */
    public void incrementAlertCount() {
        this.alertCount = (this.alertCount == null ? 0 : this.alertCount) + 1;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentSession that = (ExperimentSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
    
    @Override
    public String toString() {
        return "ExperimentSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", experimentType='" + experimentType + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}