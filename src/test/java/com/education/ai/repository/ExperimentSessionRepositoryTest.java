package com.education.ai.repository;

import com.education.ai.model.ExperimentSession;
import com.education.ai.model.SessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实验会话Repository单元测试
 * 测试实验会话数据访问层的CRUD操作和会话管理相关查询方法
 */
class ExperimentSessionRepositoryTest extends RepositoryTestBase {
    
    @Autowired
    private ExperimentSessionRepository experimentSessionRepository;
    
    @Test
    void testSaveAndFindExperimentSession() {
        // 创建测试实验会话
        ExperimentSession session = new ExperimentSession("session123", "user123", "chemistry_experiment");
        session.setExperimentName("酸碱中和实验");
        session.setDescription("测试酸碱中和反应");
        session.setStatus(SessionStatus.CREATED);
        session.setStartTime(LocalDateTime.now());
        session.setTotalDataPoints(0);
        session.setAlertCount(0);
        
        // 保存会话
        ExperimentSession savedSession = experimentSessionRepository.save(session);
        flushAndClear();
        
        // 验证保存结果
        assertNotNull(savedSession);
        assertEquals("session123", savedSession.getSessionId());
        assertEquals("user123", savedSession.getUserId());
        assertEquals("chemistry_experiment", savedSession.getExperimentType());
        assertEquals("酸碱中和实验", savedSession.getExperimentName());
        assertEquals(SessionStatus.CREATED, savedSession.getStatus());
        
        // 测试根据ID查找
        Optional<ExperimentSession> foundSession = experimentSessionRepository.findById("session123");
        assertTrue(foundSession.isPresent());
        assertEquals("chemistry_experiment", foundSession.get().getExperimentType());
    }
    
    @Test
    void testFindByUserId() {
        // 创建不同用户的实验会话
        ExperimentSession session1 = createTestSession("session1", "user1", "chemistry");
        ExperimentSession session2 = createTestSession("session2", "user1", "physics");
        ExperimentSession session3 = createTestSession("session3", "user2", "biology");
        
        experimentSessionRepository.save(session1);
        experimentSessionRepository.save(session2);
        experimentSessionRepository.save(session3);
        flushAndClear();
        
        // 测试根据用户ID查找
        List<ExperimentSession> user1Sessions = experimentSessionRepository.findByUserId("user1");
        assertEquals(2, user1Sessions.size());
        
        List<ExperimentSession> user2Sessions = experimentSessionRepository.findByUserId("user2");
        assertEquals(1, user2Sessions.size());
        
        // 测试分页查询
        Page<ExperimentSession> page = experimentSessionRepository.findByUserId("user1", PageRequest.of(0, 1));
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }
    
    @Test
    void testFindByStatus() {
        // 创建不同状态的实验会话
        ExperimentSession createdSession = createTestSession("session1", "user1", "chemistry");
        createdSession.setStatus(SessionStatus.CREATED);
        
        ExperimentSession runningSession = createTestSession("session2", "user1", "physics");
        runningSession.setStatus(SessionStatus.RUNNING);
        
        ExperimentSession completedSession = createTestSession("session3", "user1", "biology");
        completedSession.setStatus(SessionStatus.COMPLETED);
        
        experimentSessionRepository.save(createdSession);
        experimentSessionRepository.save(runningSession);
        experimentSessionRepository.save(completedSession);
        flushAndClear();
        
        // 测试根据状态查找
        List<ExperimentSession> runningSessions = experimentSessionRepository.findByStatus(SessionStatus.RUNNING);
        assertEquals(1, runningSessions.size());
        assertEquals("session2", runningSessions.get(0).getSessionId());
        
        List<ExperimentSession> completedSessions = experimentSessionRepository.findByStatus(SessionStatus.COMPLETED);
        assertEquals(1, completedSessions.size());
        assertEquals("session3", completedSessions.get(0).getSessionId());
    }
    
    @Test
    void testFindByUserIdAndStatus() {
        // 创建测试数据
        ExperimentSession session1 = createTestSession("session1", "user1", "chemistry");
        session1.setStatus(SessionStatus.RUNNING);
        
        ExperimentSession session2 = createTestSession("session2", "user1", "physics");
        session2.setStatus(SessionStatus.COMPLETED);
        
        ExperimentSession session3 = createTestSession("session3", "user2", "biology");
        session3.setStatus(SessionStatus.RUNNING);
        
        experimentSessionRepository.save(session1);
        experimentSessionRepository.save(session2);
        experimentSessionRepository.save(session3);
        flushAndClear();
        
        // 测试根据用户ID和状态查找
        List<ExperimentSession> user1RunningSessions = 
            experimentSessionRepository.findByUserIdAndStatus("user1", SessionStatus.RUNNING);
        assertEquals(1, user1RunningSessions.size());
        assertEquals("session1", user1RunningSessions.get(0).getSessionId());
        
        List<ExperimentSession> user1CompletedSessions = 
            experimentSessionRepository.findByUserIdAndStatus("user1", SessionStatus.COMPLETED);
        assertEquals(1, user1CompletedSessions.size());
        assertEquals("session2", user1CompletedSessions.get(0).getSessionId());
    }
    
    @Test
    void testFindByExperimentType() {
        // 创建不同实验类型的会话
        ExperimentSession chemSession1 = createTestSession("session1", "user1", "chemistry");
        ExperimentSession chemSession2 = createTestSession("session2", "user2", "chemistry");
        ExperimentSession physSession = createTestSession("session3", "user1", "physics");
        
        experimentSessionRepository.save(chemSession1);
        experimentSessionRepository.save(chemSession2);
        experimentSessionRepository.save(physSession);
        flushAndClear();
        
        // 测试根据实验类型查找
        List<ExperimentSession> chemistrySessions = 
            experimentSessionRepository.findByExperimentType("chemistry");
        assertEquals(2, chemistrySessions.size());
        
        List<ExperimentSession> physicsSessions = 
            experimentSessionRepository.findByExperimentType("physics");
        assertEquals(1, physicsSessions.size());
    }
    
    @Test
    void testFindActiveSessions() {
        // 创建不同状态的会话
        ExperimentSession runningSession = createTestSession("session1", "user1", "chemistry");
        runningSession.setStatus(SessionStatus.RUNNING);
        
        ExperimentSession pausedSession = createTestSession("session2", "user1", "physics");
        pausedSession.setStatus(SessionStatus.PAUSED);
        
        ExperimentSession completedSession = createTestSession("session3", "user1", "biology");
        completedSession.setStatus(SessionStatus.COMPLETED);
        
        ExperimentSession createdSession = createTestSession("session4", "user1", "math");
        createdSession.setStatus(SessionStatus.CREATED);
        
        experimentSessionRepository.save(runningSession);
        experimentSessionRepository.save(pausedSession);
        experimentSessionRepository.save(completedSession);
        experimentSessionRepository.save(createdSession);
        flushAndClear();
        
        // 测试查找活跃会话（RUNNING 和 PAUSED）
        List<ExperimentSession> activeSessions = experimentSessionRepository.findActiveSessions();
        assertEquals(2, activeSessions.size());
        
        // 验证返回的都是活跃状态
        for (ExperimentSession session : activeSessions) {
            assertTrue(session.getStatus() == SessionStatus.RUNNING || 
                      session.getStatus() == SessionStatus.PAUSED);
        }
    }
    
    @Test
    void testFindActiveSessionsByUser() {
        // 创建测试数据
        ExperimentSession user1Running = createTestSession("session1", "user1", "chemistry");
        user1Running.setStatus(SessionStatus.RUNNING);
        
        ExperimentSession user1Paused = createTestSession("session2", "user1", "physics");
        user1Paused.setStatus(SessionStatus.PAUSED);
        
        ExperimentSession user1Completed = createTestSession("session3", "user1", "biology");
        user1Completed.setStatus(SessionStatus.COMPLETED);
        
        ExperimentSession user2Running = createTestSession("session4", "user2", "math");
        user2Running.setStatus(SessionStatus.RUNNING);
        
        experimentSessionRepository.save(user1Running);
        experimentSessionRepository.save(user1Paused);
        experimentSessionRepository.save(user1Completed);
        experimentSessionRepository.save(user2Running);
        flushAndClear();
        
        // 测试查找用户的活跃会话
        List<ExperimentSession> user1ActiveSessions = 
            experimentSessionRepository.findActiveSessionsByUser("user1");
        assertEquals(2, user1ActiveSessions.size());
        
        List<ExperimentSession> user2ActiveSessions = 
            experimentSessionRepository.findActiveSessionsByUser("user2");
        assertEquals(1, user2ActiveSessions.size());
    }
    
    @Test
    void testFindByStartTimeBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        
        // 创建不同开始时间的会话
        ExperimentSession oldSession = createTestSession("session1", "user1", "chemistry");
        oldSession.setStartTime(twoHoursAgo);
        
        ExperimentSession recentSession = createTestSession("session2", "user1", "physics");
        recentSession.setStartTime(oneHourAgo.plusMinutes(30));
        
        ExperimentSession newSession = createTestSession("session3", "user1", "biology");
        newSession.setStartTime(now.minusMinutes(10)); // 确保在范围内
        
        experimentSessionRepository.save(oldSession);
        experimentSessionRepository.save(recentSession);
        experimentSessionRepository.save(newSession);
        flushAndClear();
        
        // 测试时间范围查询
        List<ExperimentSession> recentSessions = 
            experimentSessionRepository.findByStartTimeBetween(oneHourAgo, now.plusMinutes(1));
        assertEquals(2, recentSessions.size());
    }
    
    @Test
    void testFindTimeoutSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutThreshold = now.minusHours(2);
        
        // 创建测试数据
        ExperimentSession timeoutSession = createTestSession("session1", "user1", "chemistry");
        timeoutSession.setStatus(SessionStatus.RUNNING);
        timeoutSession.setStartTime(now.minusHours(3)); // 超时
        
        ExperimentSession normalSession = createTestSession("session2", "user1", "physics");
        normalSession.setStatus(SessionStatus.RUNNING);
        normalSession.setStartTime(now.minusMinutes(30)); // 正常
        
        ExperimentSession completedSession = createTestSession("session3", "user1", "biology");
        completedSession.setStatus(SessionStatus.COMPLETED);
        completedSession.setStartTime(now.minusHours(3)); // 已完成，不算超时
        
        experimentSessionRepository.save(timeoutSession);
        experimentSessionRepository.save(normalSession);
        experimentSessionRepository.save(completedSession);
        flushAndClear();
        
        // 测试查找超时会话
        List<ExperimentSession> timeoutSessions = 
            experimentSessionRepository.findTimeoutSessions(timeoutThreshold);
        assertEquals(1, timeoutSessions.size());
        assertEquals("session1", timeoutSessions.get(0).getSessionId());
    }
    
    @Test
    void testCountMethods() {
        // 创建测试数据
        ExperimentSession session1 = createTestSession("session1", "user1", "chemistry");
        session1.setStatus(SessionStatus.RUNNING);
        
        ExperimentSession session2 = createTestSession("session2", "user1", "physics");
        session2.setStatus(SessionStatus.PAUSED);
        
        ExperimentSession session3 = createTestSession("session3", "user2", "biology");
        session3.setStatus(SessionStatus.COMPLETED);
        
        experimentSessionRepository.save(session1);
        experimentSessionRepository.save(session2);
        experimentSessionRepository.save(session3);
        flushAndClear();
        
        // 测试用户会话计数
        long user1Count = experimentSessionRepository.countByUserId("user1");
        assertEquals(2, user1Count);
        
        // 测试状态会话计数
        long runningCount = experimentSessionRepository.countByStatus(SessionStatus.RUNNING);
        assertEquals(1, runningCount);
        
        // 测试活跃会话计数
        long activeCount = experimentSessionRepository.countActiveSessions();
        assertEquals(2, activeCount); // RUNNING + PAUSED
    }
    
    @Test
    void testUpdateSessionStatus() {
        // 创建测试会话
        ExperimentSession session = createTestSession("session123", "user123", "chemistry");
        session.setStatus(SessionStatus.CREATED);
        experimentSessionRepository.save(session);
        flushAndClear();
        
        // 更新会话状态
        LocalDateTime updateTime = LocalDateTime.now();
        int updatedRows = experimentSessionRepository.updateSessionStatus(
            "session123", SessionStatus.RUNNING, updateTime);
        assertEquals(1, updatedRows);
        
        flushAndClear();
        
        // 验证更新结果
        Optional<ExperimentSession> updatedSession = experimentSessionRepository.findById("session123");
        assertTrue(updatedSession.isPresent());
        assertEquals(SessionStatus.RUNNING, updatedSession.get().getStatus());
    }
    
    @Test
    void testIncrementDataPoints() {
        // 创建测试会话
        ExperimentSession session = createTestSession("session123", "user123", "chemistry");
        session.setTotalDataPoints(10);
        experimentSessionRepository.save(session);
        flushAndClear();
        
        // 增加数据点计数
        int updatedRows = experimentSessionRepository.incrementDataPoints("session123");
        assertEquals(1, updatedRows);
        
        flushAndClear();
        
        // 验证更新结果
        Optional<ExperimentSession> updatedSession = experimentSessionRepository.findById("session123");
        assertTrue(updatedSession.isPresent());
        assertEquals(11, updatedSession.get().getTotalDataPoints());
    }
    
    @Test
    void testIncrementAlertCount() {
        // 创建测试会话
        ExperimentSession session = createTestSession("session123", "user123", "chemistry");
        session.setAlertCount(5);
        experimentSessionRepository.save(session);
        flushAndClear();
        
        // 增加警告计数
        int updatedRows = experimentSessionRepository.incrementAlertCount("session123");
        assertEquals(1, updatedRows);
        
        flushAndClear();
        
        // 验证更新结果
        Optional<ExperimentSession> updatedSession = experimentSessionRepository.findById("session123");
        assertTrue(updatedSession.isPresent());
        assertEquals(6, updatedSession.get().getAlertCount());
    }
    
    @Test
    void testDeleteExperimentSession() {
        // 创建测试会话
        ExperimentSession session = createTestSession("session123", "user123", "chemistry");
        experimentSessionRepository.save(session);
        flushAndClear();
        
        // 验证会话存在
        assertTrue(experimentSessionRepository.existsById("session123"));
        
        // 删除会话
        experimentSessionRepository.deleteById("session123");
        flushAndClear();
        
        // 验证会话已删除
        assertFalse(experimentSessionRepository.existsById("session123"));
    }
    
    /**
     * 创建测试实验会话的辅助方法
     */
    private ExperimentSession createTestSession(String sessionId, String userId, String experimentType) {
        ExperimentSession session = new ExperimentSession(sessionId, userId, experimentType);
        session.setStatus(SessionStatus.CREATED);
        session.setStartTime(LocalDateTime.now());
        session.setTotalDataPoints(0);
        session.setAlertCount(0);
        return session;
    }
}