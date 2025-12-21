package com.education.ai.repository;

import com.education.ai.model.ExperimentSession;
import com.education.ai.model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 实验会话数据访问接口
 * 提供实验会话的CRUD操作和会话管理相关查询方法
 */
@Repository
public interface ExperimentSessionRepository extends JpaRepository<ExperimentSession, String> {
    
    /**
     * 根据用户ID查找实验会话
     */
    List<ExperimentSession> findByUserId(String userId);
    
    /**
     * 根据用户ID分页查找实验会话
     */
    Page<ExperimentSession> findByUserId(String userId, Pageable pageable);
    
    /**
     * 根据状态查找实验会话
     */
    List<ExperimentSession> findByStatus(SessionStatus status);
    
    /**
     * 根据用户ID和状态查找实验会话
     */
    List<ExperimentSession> findByUserIdAndStatus(String userId, SessionStatus status);
    
    /**
     * 根据实验类型查找会话
     */
    List<ExperimentSession> findByExperimentType(String experimentType);
    
    /**
     * 查找活跃的实验会话
     */
    @Query("SELECT e FROM ExperimentSession e WHERE e.status IN ('RUNNING', 'PAUSED')")
    List<ExperimentSession> findActiveSessions();
    
    /**
     * 查找用户的活跃会话
     */
    @Query("SELECT e FROM ExperimentSession e WHERE e.userId = :userId AND e.status IN ('RUNNING', 'PAUSED')")
    List<ExperimentSession> findActiveSessionsByUser(@Param("userId") String userId);
    
    /**
     * 查找指定时间范围内的会话
     */
    List<ExperimentSession> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找超时的会话
     */
    @Query("SELECT e FROM ExperimentSession e WHERE e.status = 'RUNNING' AND " +
           "e.startTime < :timeoutThreshold")
    List<ExperimentSession> findTimeoutSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    /**
     * 根据WebSocket连接ID查找会话
     */
    Optional<ExperimentSession> findByWebsocketConnectionId(String connectionId);
    
    /**
     * 统计用户的会话数量
     */
    long countByUserId(String userId);
    
    /**
     * 统计指定状态的会话数量
     */
    long countByStatus(SessionStatus status);
    
    /**
     * 统计活跃会话数量
     */
    @Query("SELECT COUNT(e) FROM ExperimentSession e WHERE e.status IN ('RUNNING', 'PAUSED')")
    long countActiveSessions();
    
    /**
     * 查找用户最近的会话
     */
    List<ExperimentSession> findTop10ByUserIdOrderByStartTimeDesc(String userId);
    
    /**
     * 计算平均会话持续时间
     */
    @Query("SELECT AVG(e.durationMinutes) FROM ExperimentSession e " +
           "WHERE e.status = 'COMPLETED' AND e.durationMinutes IS NOT NULL")
    Double getAverageSessionDuration();
    
    /**
     * 统计实验类型使用情况
     */
    @Query("SELECT e.experimentType, COUNT(e) FROM ExperimentSession e " +
           "GROUP BY e.experimentType ORDER BY COUNT(e) DESC")
    List<Object[]> getExperimentTypeStatistics();
    
    /**
     * 查找长时间运行的会话
     */
    @Query("SELECT e FROM ExperimentSession e WHERE e.status = 'RUNNING' AND " +
           "e.startTime < :cutoffTime ORDER BY e.startTime")
    List<ExperimentSession> findLongRunningSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 更新会话状态
     */
    @Modifying
    @Query("UPDATE ExperimentSession e SET e.status = :status, e.lastUpdated = :updateTime " +
           "WHERE e.sessionId = :sessionId")
    int updateSessionStatus(@Param("sessionId") String sessionId, 
                           @Param("status") SessionStatus status,
                           @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 更新会话结束时间
     */
    @Modifying
    @Query("UPDATE ExperimentSession e SET e.endTime = :endTime, e.status = :status, " +
           "e.durationMinutes = :duration WHERE e.sessionId = :sessionId")
    int updateSessionEnd(@Param("sessionId") String sessionId,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("status") SessionStatus status,
                        @Param("duration") Integer duration);
    
    /**
     * 增加数据点计数
     */
    @Modifying
    @Query("UPDATE ExperimentSession e SET e.totalDataPoints = e.totalDataPoints + 1 " +
           "WHERE e.sessionId = :sessionId")
    int incrementDataPoints(@Param("sessionId") String sessionId);
    
    /**
     * 增加警告计数
     */
    @Modifying
    @Query("UPDATE ExperimentSession e SET e.alertCount = e.alertCount + 1 " +
           "WHERE e.sessionId = :sessionId")
    int incrementAlertCount(@Param("sessionId") String sessionId);
    
    /**
     * 批量更新超时会话状态
     */
    @Modifying
    @Query("UPDATE ExperimentSession e SET e.status = 'TIMEOUT', e.endTime = :currentTime " +
           "WHERE e.status = 'RUNNING' AND e.startTime < :timeoutThreshold")
    int markTimeoutSessions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold,
                           @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 删除指定时间之前的已完成会话
     */
    void deleteByStatusAndEndTimeBefore(SessionStatus status, LocalDateTime cutoffDate);
}