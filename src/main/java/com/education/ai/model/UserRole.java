package com.education.ai.model;

/**
 * 用户角色枚举
 * 定义系统中不同类型的用户角色
 */
public enum UserRole {
    /**
     * 学生用户 - 可以使用所有学习功能
     */
    STUDENT("学生"),
    
    /**
     * 教师用户 - 可以管理学生和查看学习数据
     */
    TEACHER("教师"),
    
    /**
     * 管理员用户 - 拥有系统管理权限
     */
    ADMIN("管理员"),
    
    /**
     * 教育机构用户 - 可以使用实验检测等高级功能
     */
    INSTITUTION("教育机构");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 检查角色是否有管理权限
     */
    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }
    
    /**
     * 检查角色是否可以使用实验检测功能
     */
    public boolean canUseExperimentDetection() {
        return this == TEACHER || this == ADMIN || this == INSTITUTION;
    }
    
    /**
     * 检查角色是否可以查看其他用户数据
     */
    public boolean canViewOtherUsersData() {
        return this == TEACHER || this == ADMIN || this == INSTITUTION;
    }
}