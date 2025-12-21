package com.education.ai.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据模型单元测试
 * 测试实体类的基本功能和验证逻辑
 */
class DataModelTest {
    
    @Test
    void testUserEntity() {
        // 创建用户实体
        User user = new User("user123", "testuser", "test@example.com");
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setFullName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        
        // 验证基本属性
        assertEquals("user123", user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(UserRole.STUDENT, user.getRole());
        assertTrue(user.getActive());
        assertEquals("Test User", user.getFullName());
        assertNotNull(user.getCreatedAt());
        
        // 测试equals和hashCode
        User user2 = new User("user123", "testuser2", "test2@example.com");
        assertEquals(user, user2); // 相同的userId应该相等
        assertEquals(user.hashCode(), user2.hashCode());
        
        // 测试toString
        assertNotNull(user.toString());
        assertTrue(user.toString().contains("user123"));
    }
    
    @Test
    void testUserRoleEnum() {
        // 测试角色权限
        assertTrue(UserRole.ADMIN.hasAdminPrivileges());
        assertFalse(UserRole.STUDENT.hasAdminPrivileges());
        
        assertTrue(UserRole.TEACHER.canUseExperimentDetection());
        assertTrue(UserRole.INSTITUTION.canUseExperimentDetection());
        assertFalse(UserRole.STUDENT.canUseExperimentDetection());
        
        assertTrue(UserRole.ADMIN.canViewOtherUsersData());
        assertFalse(UserRole.STUDENT.canViewOtherUsersData());
        
        // 测试显示名称
        assertEquals("学生", UserRole.STUDENT.getDisplayName());
        assertEquals("教师", UserRole.TEACHER.getDisplayName());
        assertEquals("管理员", UserRole.ADMIN.getDisplayName());
        assertEquals("教育机构", UserRole.INSTITUTION.getDisplayName());
    }
    
    @Test
    void testGestureAnalysisRecordEntity() {
        // 创建手势分析记录
        GestureAnalysisRecord record = new GestureAnalysisRecord("analysis123", "user123", "{\"x\":100,\"y\":200}");
        record.setConfidence(0.95);
        record.setProcessingTimeMs(2500L);
        record.setStatus(AnalysisStatus.SUCCESS);
        record.setExplanation("这是一个数学公式的解释");
        record.setSubjectArea("mathematics");
        record.setCreatedAt(LocalDateTime.now());
        
        // 验证基本属性
        assertEquals("analysis123", record.getAnalysisId());
        assertEquals("user123", record.getUserId());
        assertEquals("{\"x\":100,\"y\":200}", record.getGestureData());
        assertEquals(0.95, record.getConfidence());
        assertEquals(2500L, record.getProcessingTimeMs());
        assertEquals(AnalysisStatus.SUCCESS, record.getStatus());
        assertEquals("这是一个数学公式的解释", record.getExplanation());
        assertEquals("mathematics", record.getSubjectArea());
        
        // 测试equals和hashCode
        GestureAnalysisRecord record2 = new GestureAnalysisRecord("analysis123", "user456", "{\"x\":200,\"y\":300}");
        assertEquals(record, record2); // 相同的analysisId应该相等
        assertEquals(record.hashCode(), record2.hashCode());
    }
    
    @Test
    void testAnalysisStatusEnum() {
        // 测试状态判断方法
        assertTrue(AnalysisStatus.SUCCESS.isFinalStatus());
        assertTrue(AnalysisStatus.FAILED.isFinalStatus());
        assertFalse(AnalysisStatus.PENDING.isFinalStatus());
        assertFalse(AnalysisStatus.PROCESSING.isFinalStatus());
        
        assertTrue(AnalysisStatus.SUCCESS.isSuccessful());
        assertFalse(AnalysisStatus.FAILED.isSuccessful());
        
        assertTrue(AnalysisStatus.FAILED.isError());
        assertTrue(AnalysisStatus.TIMEOUT.isError());
        assertFalse(AnalysisStatus.SUCCESS.isError());
        
        // 测试显示名称
        assertEquals("等待处理", AnalysisStatus.PENDING.getDisplayName());
        assertEquals("处理成功", AnalysisStatus.SUCCESS.getDisplayName());
        assertEquals("处理失败", AnalysisStatus.FAILED.getDisplayName());
    }
    
    @Test
    void testExperimentSessionEntity() {
        // 创建实验会话
        ExperimentSession session = new ExperimentSession("session123", "user123", "chemistry_experiment");
        session.setExperimentName("酸碱中和实验");
        session.setDescription("测试酸碱中和反应");
        session.setStatus(SessionStatus.RUNNING);
        session.setStartTime(LocalDateTime.now().minusMinutes(30));
        session.setTotalDataPoints(100);
        session.setAlertCount(2);
        
        // 验证基本属性
        assertEquals("session123", session.getSessionId());
        assertEquals("user123", session.getUserId());
        assertEquals("chemistry_experiment", session.getExperimentType());
        assertEquals("酸碱中和实验", session.getExperimentName());
        assertEquals("测试酸碱中和反应", session.getDescription());
        assertEquals(SessionStatus.RUNNING, session.getStatus());
        assertEquals(100, session.getTotalDataPoints());
        assertEquals(2, session.getAlertCount());
        
        // 测试计数器方法
        session.incrementDataPoints();
        assertEquals(101, session.getTotalDataPoints());
        
        session.incrementAlertCount();
        assertEquals(3, session.getAlertCount());
        
        // 测试持续时间计算
        session.setEndTime(LocalDateTime.now());
        session.calculateDuration();
        assertNotNull(session.getDurationMinutes());
        assertTrue(session.getDurationMinutes() >= 29); // 至少29分钟
    }
    
    @Test
    void testSessionStatusEnum() {
        // 测试状态判断方法
        assertTrue(SessionStatus.RUNNING.isActive());
        assertTrue(SessionStatus.PAUSED.isActive());
        assertFalse(SessionStatus.COMPLETED.isActive());
        
        assertTrue(SessionStatus.COMPLETED.isFinished());
        assertTrue(SessionStatus.CANCELLED.isFinished());
        assertFalse(SessionStatus.RUNNING.isFinished());
        
        assertTrue(SessionStatus.CREATED.canStart());
        assertFalse(SessionStatus.RUNNING.canStart());
        
        assertTrue(SessionStatus.RUNNING.canPause());
        assertFalse(SessionStatus.PAUSED.canPause());
        
        assertTrue(SessionStatus.PAUSED.canResume());
        assertFalse(SessionStatus.RUNNING.canResume());
        
        assertTrue(SessionStatus.RUNNING.canStop());
        assertTrue(SessionStatus.PAUSED.canStop());
        assertFalse(SessionStatus.COMPLETED.canStop());
    }
    
    @Test
    void testApiCallLogEntity() {
        // 创建API调用日志
        ApiCallLog log = new ApiCallLog("multimodal-api", 2500L, true);
        log.setStatusCode(200);
        log.setUserId("user123");
        log.setRequestId("req123");
        log.setEndpoint("/api/v1/analyze");
        log.setHttpMethod("POST");
        log.setRequestSizeBytes(1024L);
        log.setResponseSizeBytes(2048L);
        log.setRetryCount(0);
        log.setCallTime(LocalDateTime.now());
        
        // 验证基本属性
        assertEquals("multimodal-api", log.getApiName());
        assertEquals(2500L, log.getResponseTimeMs());
        assertTrue(log.getSuccess());
        assertEquals(200, log.getStatusCode());
        assertEquals("user123", log.getUserId());
        assertEquals("req123", log.getRequestId());
        assertEquals("/api/v1/analyze", log.getEndpoint());
        assertEquals("POST", log.getHttpMethod());
        assertEquals(1024L, log.getRequestSizeBytes());
        assertEquals(2048L, log.getResponseSizeBytes());
        assertEquals(0, log.getRetryCount());
        
        // 测试健康状态判断
        assertTrue(log.isHealthy()); // 成功且响应时间 < 5秒
        assertFalse(log.isSlowQuery()); // 响应时间 < 3秒
        
        // 测试慢查询
        log.setResponseTimeMs(4000L);
        assertTrue(log.isSlowQuery()); // 响应时间 > 3秒
        assertTrue(log.isHealthy()); // 仍然健康，因为 < 5秒
        
        // 测试不健康状态
        log.setResponseTimeMs(6000L);
        assertFalse(log.isHealthy()); // 响应时间 > 5秒
        
        log.setResponseTimeMs(2000L);
        log.setSuccess(false);
        assertFalse(log.isHealthy()); // 失败的调用
    }
}