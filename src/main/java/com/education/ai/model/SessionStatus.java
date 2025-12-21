package com.education.ai.model;

/**
 * 会话状态枚举
 * 定义实验会话的各种状态
 */
public enum SessionStatus {
    /**
     * 已创建，等待开始
     */
    CREATED("已创建"),//枚举类:设置的每一个常量都是枚举类的一个对象
    
    /**
     * 正在运行
     */
    RUNNING("正在运行"),
    
    /**
     * 已暂停
     */
    PAUSED("已暂停"),
    
    /**
     * 正常结束
     */
    COMPLETED("正常结束"),
    
    /**
     * 异常终止
     */
    TERMINATED("异常终止"),
    
    /**
     * 超时结束
     */
    TIMEOUT("超时结束"),
    
    /**
     * 用户取消
     */
    CANCELLED("用户取消"),
    
    /**
     * 系统错误
     */
    ERROR("系统错误");
    
    private final String displayName;
    
    SessionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检查会话是否处于活跃状态
     */
    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
    
    /**
     * 检查会话是否已结束
     */
    public boolean isFinished() {
        return this == COMPLETED || this == TERMINATED || 
               this == TIMEOUT || this == CANCELLED || this == ERROR;
    }
    
    /**
     * 检查会话是否可以开始
     */
    public boolean canStart() {
        return this == CREATED;
    }
    
    /**
     * 检查会话是否可以暂停
     */
    public boolean canPause() {
        return this == RUNNING;
    }
    
    /**
     * 检查会话是否可以恢复
     */
    public boolean canResume() {
        return this == PAUSED;
    }
    
    /**
     * 检查会话是否可以停止
     */
    public boolean canStop() {
        return this == RUNNING || this == PAUSED;
    }
}