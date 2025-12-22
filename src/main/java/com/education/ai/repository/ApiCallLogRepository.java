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
    @Query("SELECT AVG(a.responseTimeMs) FROM ApiCallLog a WHERE a.apiName = :apiName AND a.success = :success")
    Double getAverageResponseTime(@Param("apiName") String apiName, @Param("success") Boolean success);
    
    /**
     * 计算API平均响应时间（只计算成功的调用）
     */
    default Double getAverageResponseTime(String apiName) {
        return getAverageResponseTime(apiName, true);
    }
    
    /**
     * 计算API成功率
     */
    @Query("SELECT (CAST(COUNT(CASE WHEN a.success = :success THEN 1 END) AS DOUBLE) * 100.0 / COUNT(a)) " +
           "FROM ApiCallLog a WHERE a.apiName = :apiName")
    Double getSuccessRate(@Param("apiName") String apiName, @Param("success") Boolean success);
    
    /**
     * 计算API成功率
     */
    default Double getSuccessRate(String apiName) {
        return getSuccessRate(apiName, true);
    }
    
    /**
     * 获取API调用统计信息
     */
    @Query("SELECT a.apiName, COUNT(a) as totalCalls, " +
           "SUM(CASE WHEN a.success = :success THEN 1 ELSE 0 END) as successCalls, " +
           "AVG(a.responseTimeMs) as avgResponseTime " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName")
    List<Object[]> getApiStatistics(@Param("startTime") LocalDateTime startTime, @Param("success") Boolean success);
    
    /**
     * 获取API调用统计信息（默认统计成功的调用）
     */
    default List<Object[]> getApiStatistics(LocalDateTime startTime) {
        return getApiStatistics(startTime, true);
    }
    
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
           "(CAST(COUNT(CASE WHEN a.success = :success THEN 1 END) AS DOUBLE) * 100.0 / COUNT(a)) as errorRate " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName " +
           "HAVING (CAST(COUNT(CASE WHEN a.success = :success THEN 1 END) AS DOUBLE) * 100.0 / COUNT(a)) > :threshold " +
           "ORDER BY (CAST(COUNT(CASE WHEN a.success = :success THEN 1 END) AS DOUBLE) * 100.0 / COUNT(a)) DESC")
    List<Object[]> findHighErrorRateApis(@Param("startTime") LocalDateTime startTime, 
                                        @Param("threshold") Double threshold,
                                        @Param("success") Boolean success);
    
    /**
     * 查找错误率高的API（默认查找失败的调用）
     */
    default List<Object[]> findHighErrorRateApis(LocalDateTime startTime, Double threshold) {
        return findHighErrorRateApis(startTime, threshold, false);
    }
    
    /**
     * 查找响应时间异常的API调用
     */
    @Query("SELECT a FROM ApiCallLog a WHERE a.responseTimeMs > " +
           "(SELECT AVG(b.responseTimeMs) * 3 FROM ApiCallLog b WHERE b.apiName = a.apiName AND b.success = :success) " +
           "ORDER BY a.responseTimeMs DESC")
    List<ApiCallLog> findAbnormalResponseTimes(@Param("success") Boolean success);
    
    /**
     * 查找响应时间异常的API调用（默认只查看成功的调用）
     */
    default List<ApiCallLog> findAbnormalResponseTimes() {
        return findAbnormalResponseTimes(true);
    }
    
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
           "(CAST(SUM(CASE WHEN a.success = :success THEN 1 ELSE 0 END) AS DOUBLE) * 100.0 / COUNT(a)) as successRate " +
           "FROM ApiCallLog a " +
           "WHERE a.callTime >= :startTime " +
           "GROUP BY a.apiName " +
           "ORDER BY a.apiName")
    List<Object[]> getApiHealthReport(@Param("startTime") LocalDateTime startTime, @Param("success") Boolean success);
    
    /**
     * 获取API健康状态报告（默认统计成功的调用）
     */
    default List<Object[]> getApiHealthReport(LocalDateTime startTime) {
        return getApiHealthReport(startTime, true);
    }
    
    /**
     * 根据API名称包含指定字符串和时间范围查找调用日志
     */
    List<ApiCallLog> findByApiNameContainingAndCallTimeAfter(String apiName, LocalDateTime startTime);
    
    /**
     * 查找指定时间之后的所有调用日志
     */
    List<ApiCallLog> findByCallTimeAfter(LocalDateTime startTime);
}