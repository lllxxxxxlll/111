package com.education.ai.repository;

import com.education.ai.model.ApiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API调用日志数据访问接口
 * 提供API调用日志的CRUD操作和监控统计查询方法
 */
@Repository
public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {
    
    /**
     * 根据API名称查找调用日志
     */
    List<ApiCallLog> findByApiName(String apiName);
    
    /**
     * 根据API名称分页查找调用日志
     */
    Page<ApiCallLog> findByApiName(String apiName, Pageable pageable);
    
    /**
     * 根据成功状态查找调用日志
     */
    List<ApiCallLog> findBySuccess(Boolean success);
    
    /**
     * 根据API名称和成功状态查找调用日志
     */
    List<ApiCallLog> findByApiNameAndSuccess(String apiName, Boolean success);
    
    /**
     * 查找指定时间范围内的调用日志
     */
    List<ApiCallLog> findByCallTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据用户ID查找调用日志
     */
    List<ApiCallLog> findByUserId(String userId);
    
    /**
     * 根据请求ID查找调用日志
     */
    List<ApiCallLog> findByRequestId(String requestId);
    
    /**
     * 查找响应时间超过指定值的调用日志
     */
    List<ApiCallLog> findByResponseTimeMsGreaterThan(Long responseTimeMs);
    
    /**
     * 查找失败的API调用
     */
    List<ApiCallLog> findBySuccessFalse();
    
    /**
     * 查找慢查询（响应时间超过3秒）
     */
    @Query("SELECT a FROM ApiCallLog a WHERE a.responseTimeMs > 3000 ORDER BY a.responseTimeMs DESC")
    List<ApiCallLog> findSlowQueries();
    
    /**
     * 统计API调用总数
     */
    long countByApiName(String apiName);
    
    /**
     * 统计成功的API调用数
     */
    long countByApiNameAndSuccess(String apiName, Boolean success);
    
    /**
     * 统计指定时间范围内的调用数
     */
    long countByApiNameAndCallTimeBetween(String apiName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算API平均响应时间
     */
    @Query("SELECT AVG(a.responseTimeMs) FROM ApiCallLog a WHERE a.apiName = :apiName AND a.success = TRUE")
    Double getAverageResponseTime(@Param("apiName") String apiName);
    
    /**
     * 计算API成功率
     */
    @Query("SELECT (COUNT(a) * 100.0 / (SELECT COUNT(b) FROM ApiCallLog b WHERE b.apiName = :apiName)) " +
           "FROM ApiCallLog a WHERE a.apiName = :apiName AND a.success = TRUE")
    Double getSuccessRate(@Param("apiName") String apiName);
    
    /**
     * 获取API调用统计信息
     */
    @Query("SELECT a.apiName, COUNT(a) as totalCalls, " +
           "SUM(CASE WHEN a.success = TRUE THEN 1 ELSE 0 END) as successCalls, " +
           "AVG(a.responseTimeMs) as avgResponseTime " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName")
    List<Object[]> getApiStatistics(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 获取每小时API调用量统计
     */
    @Query("SELECT EXTRACT(HOUR FROM a.callTime) as hour, COUNT(a) as callCount " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY EXTRACT(HOUR FROM a.callTime) " +
           "ORDER BY EXTRACT(HOUR FROM a.callTime)")
    List<Object[]> getHourlyCallStatistics(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 查找最近的API调用记录
     */
    List<ApiCallLog> findTop100ByOrderByCallTimeDesc();
    
    /**
     * 查找指定API最近的调用记录
     */
    List<ApiCallLog> findTop50ByApiNameOrderByCallTimeDesc(String apiName);
    
    /**
     * 查找错误率高的API
     */
    @Query("SELECT a.apiName, " +
           "(COUNT(CASE WHEN a.success = FALSE THEN 1 END) * 100.0 / COUNT(a)) as errorRate " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName " +
           "HAVING (COUNT(CASE WHEN a.success = FALSE THEN 1 END) * 100.0 / COUNT(a)) > :threshold " +
           "ORDER BY (COUNT(CASE WHEN a.success = FALSE THEN 1 END) * 100.0 / COUNT(a)) DESC")
    List<Object[]> findHighErrorRateApis(@Param("startTime") LocalDateTime startTime, 
                                        @Param("threshold") Double threshold);
    
    /**
     * 查找响应时间异常的API调用
     */
    @Query("SELECT a FROM ApiCallLog a WHERE a.responseTimeMs > " +
           "(SELECT AVG(b.responseTimeMs) * 3 FROM ApiCallLog b WHERE b.apiName = a.apiName AND b.success = TRUE) " +
           "ORDER BY a.responseTimeMs DESC")
    List<ApiCallLog> findAbnormalResponseTimes();
    
    /**
     * 删除指定时间之前的日志记录
     */
    void deleteByCallTimeBefore(LocalDateTime cutoffDate);
    
    /**
     * 获取API健康状态报告
     */
    @Query("SELECT a.apiName, " +
           "COUNT(a) as totalCalls, " +
           "AVG(a.responseTimeMs) as avgResponseTime, " +
           "MAX(a.responseTimeMs) as maxResponseTime, " +
           "MIN(a.responseTimeMs) as minResponseTime, " +
           "(SUM(CASE WHEN a.success = TRUE THEN 1 ELSE 0 END) * 100.0 / COUNT(a)) as successRate " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName " +
           "ORDER BY a.apiName")
    List<Object[]> getApiHealthReport(@Param("startTime") LocalDateTime startTime);
}