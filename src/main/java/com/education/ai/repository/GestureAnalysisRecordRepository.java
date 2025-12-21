package com.education.ai.repository;

import com.education.ai.model.AnalysisStatus;
import com.education.ai.model.GestureAnalysisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 手势分析记录数据访问接口
 * 提供手势分析记录的CRUD操作和统计查询方法
 */
@Repository
public interface GestureAnalysisRecordRepository extends JpaRepository<GestureAnalysisRecord, String> {
    
    /**
     * 根据用户ID查找分析记录
     */
    List<GestureAnalysisRecord> findByUserId(String userId);
    
    /**
     * 根据用户ID分页查找分析记录
     */
    Page<GestureAnalysisRecord> findByUserId(String userId, Pageable pageable);
    
    /**
     * 根据状态查找分析记录
     */
    List<GestureAnalysisRecord> findByStatus(AnalysisStatus status);
    
    /**
     * 根据用户ID和状态查找分析记录
     */
    List<GestureAnalysisRecord> findByUserIdAndStatus(String userId, AnalysisStatus status);
    
    /**
     * 查找指定时间范围内的分析记录
     */
    List<GestureAnalysisRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据用户ID和时间范围查找分析记录
     */
    List<GestureAnalysisRecord> findByUserIdAndCreatedAtBetween(
            String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找置信度高于指定值的记录
     */
    List<GestureAnalysisRecord> findByConfidenceGreaterThan(Double confidence);
    
    /**
     * 查找处理时间超过指定值的记录
     */
    List<GestureAnalysisRecord> findByProcessingTimeMsGreaterThan(Long processingTimeMs);
    
    /**
     * 根据学科领域查找记录
     */
    List<GestureAnalysisRecord> findBySubjectArea(String subjectArea);
    
    /**
     * 统计用户的分析记录数量
     */
    long countByUserId(String userId);
    
    /**
     * 统计指定状态的记录数量
     */
    long countByStatus(AnalysisStatus status);
    
    /**
     * 统计用户成功的分析记录数量
     */
    long countByUserIdAndStatus(String userId, AnalysisStatus status);
    
    /**
     * 查找用户最近的分析记录
     */
    List<GestureAnalysisRecord> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 计算平均处理时间
     */
    @Query("SELECT AVG(g.processingTimeMs) FROM GestureAnalysisRecord g WHERE g.status = :status")
    Double getAverageProcessingTime(@Param("status") AnalysisStatus status);
    
    /**
     * 计算用户的平均置信度
     */
    @Query("SELECT AVG(g.confidence) FROM GestureAnalysisRecord g " +
           "WHERE g.userId = :userId AND g.status = 'SUCCESS'")
    Double getAverageConfidenceByUser(@Param("userId") String userId);
    
    /**
     * 统计每日分析记录数量
     */
    @Query("SELECT FORMATDATETIME(g.createdAt, 'yyyy-MM-dd') as date, COUNT(g) as count " +
           "FROM GestureAnalysisRecord g " +
           "WHERE g.createdAt >= :startDate " +
           "GROUP BY FORMATDATETIME(g.createdAt, 'yyyy-MM-dd') " +
           "ORDER BY FORMATDATETIME(g.createdAt, 'yyyy-MM-dd')")
    List<Object[]> getDailyAnalysisCount(@Param("startDate") LocalDateTime startDate);
    
    /**
     * 查找高置信度的成功记录
     */
    @Query("SELECT g FROM GestureAnalysisRecord g " +
           "WHERE g.status = 'SUCCESS' AND g.confidence >= :minConfidence " +
           "ORDER BY g.confidence DESC")
    List<GestureAnalysisRecord> findHighConfidenceRecords(@Param("minConfidence") Double minConfidence);
    
    /**
     * 查找需要重新处理的记录（失败或超时）
     */
    @Query("SELECT g FROM GestureAnalysisRecord g " +
           "WHERE g.status IN ('FAILED', 'TIMEOUT') " +
           "ORDER BY g.createdAt DESC")
    List<GestureAnalysisRecord> findRecordsForReprocessing();
    
    /**
     * 删除指定时间之前的记录
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}