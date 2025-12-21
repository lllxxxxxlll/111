package com.education.ai.model;

/**
 * 分析状态枚举
 * 定义各种分析任务的处理状态
 */
public enum AnalysisStatus {
    /**
     * 等待处理
     */
    PENDING("等待处理"),
    
    /**
     * 处理中
     */
    PROCESSING("处理中"),
    
    /**
     * 处理成功
     */
    SUCCESS("处理成功"),
    
    /**
     * 处理失败
     */
    FAILED("处理失败"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消"),
    
    /**
     * 超时
     */
    TIMEOUT("超时");
    
    private final String displayName;
    
    AnalysisStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检查状态是否为最终状态（不会再改变）
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
    
    /**
     * 检查状态是否为成功状态
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }
    
    /**
     * 检查状态是否为错误状态
     */
    public boolean isError() {
        return this == FAILED || this == TIMEOUT;
    }
}