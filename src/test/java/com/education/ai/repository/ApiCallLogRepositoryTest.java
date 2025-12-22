package com.education.ai.repository;

import com.education.ai.model.ApiCallLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API调用日志Repository单元测试
 * 测试API调用日志数据访问层的CRUD操作和统计查询方法
 */
class ApiCallLogRepositoryTest extends RepositoryTestBase {
    
    @Autowired
    private ApiCallLogRepository apiCallLogRepository;
    
    @Test
    void testSaveAndFindApiCallLog() {
        // 创建测试API调用日志
        ApiCallLog log = new ApiCallLog("multimodal-api", 2500L, true);
        log.setStatusCode(200);
        log.setUserId("user123");
        log.setRequestId("req123");
        log.setEndpoint("/api/v1/analyze");
        log.setHttpMethod("POST");
        log.setCallTime(LocalDateTime.now());
        
        // 保存日志
        ApiCallLog savedLog = apiCallLogRepository.save(log);
        flushAndClear();
        
        // 验证保存结果
        assertNotNull(savedLog);
        assertNotNull(savedLog.getLogId());
        assertEquals("multimodal-api", savedLog.getApiName());
        assertEquals(2500L, savedLog.getResponseTimeMs());
        assertTrue(savedLog.getSuccess());
        assertEquals(200, savedLog.getStatusCode());
        
        // 测试根据ID查找
        assertTrue(apiCallLogRepository.findById(savedLog.getLogId()).isPresent());
    }
    
    @Test
    void testFindByApiName() {
        // 创建不同API的调用日志
        ApiCallLog log1 = createTestApiLog("multimodal-api", 1000L, true);
        ApiCallLog log2 = createTestApiLog("multimodal-api", 2000L, false);
        ApiCallLog log3 = createTestApiLog("image-api", 1500L, true);
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试根据API名称查找
        List<ApiCallLog> multimodalLogs = apiCallLogRepository.findByApiName("multimodal-api");
        assertEquals(2, multimodalLogs.size());
        
        List<ApiCallLog> imageLogs = apiCallLogRepository.findByApiName("image-api");
        assertEquals(1, imageLogs.size());
        
        // 测试分页查询
        Page<ApiCallLog> page = apiCallLogRepository.findByApiName("multimodal-api", PageRequest.of(0, 1));
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }
    
    @Test
    void testFindBySuccess() {
        // 创建成功和失败的调用日志
        ApiCallLog successLog1 = createTestApiLog("api1", 1000L, true);
        ApiCallLog successLog2 = createTestApiLog("api2", 2000L, true);
        ApiCallLog failLog = createTestApiLog("api3", 3000L, false);
        
        apiCallLogRepository.save(successLog1);
        apiCallLogRepository.save(successLog2);
        apiCallLogRepository.save(failLog);
        flushAndClear();
        
        // 测试查找成功的调用
        List<ApiCallLog> successLogs = apiCallLogRepository.findBySuccess(true);
        assertEquals(2, successLogs.size());
        
        // 测试查找失败的调用
        List<ApiCallLog> failLogs = apiCallLogRepository.findBySuccess(false);
        assertEquals(1, failLogs.size());
        
        // 测试查找失败的调用（使用便捷方法）
        List<ApiCallLog> failLogsConvenient = apiCallLogRepository.findBySuccessFalse();
        assertEquals(1, failLogsConvenient.size());
    }
    
    @Test
    void testFindByApiNameAndSuccess() {
        // 创建测试数据
        ApiCallLog log1 = createTestApiLog("multimodal-api", 1000L, true);
        ApiCallLog log2 = createTestApiLog("multimodal-api", 2000L, false);
        ApiCallLog log3 = createTestApiLog("image-api", 1500L, true);
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试根据API名称和成功状态查找
        List<ApiCallLog> successLogs = apiCallLogRepository.findByApiNameAndSuccess("multimodal-api", true);
        assertEquals(1, successLogs.size());
        
        List<ApiCallLog> failLogs = apiCallLogRepository.findByApiNameAndSuccess("multimodal-api", false);
        assertEquals(1, failLogs.size());
    }
    
    @Test
    void testFindByCallTimeBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        
        // 创建不同时间的调用日志
        ApiCallLog oldLog = createTestApiLog("api1", 1000L, true);
        oldLog.setCallTime(twoHoursAgo);
        
        ApiCallLog recentLog = createTestApiLog("api2", 2000L, true);
        recentLog.setCallTime(oneHourAgo.plusMinutes(30));
        
        ApiCallLog newLog = createTestApiLog("api3", 3000L, true);
        newLog.setCallTime(now.minusMinutes(10)); // 确保在范围内
        
        apiCallLogRepository.save(oldLog);
        apiCallLogRepository.save(recentLog);
        apiCallLogRepository.save(newLog);
        flushAndClear();
        
        // 测试时间范围查询
        List<ApiCallLog> recentLogs = apiCallLogRepository.findByCallTimeBetween(oneHourAgo, now.plusMinutes(1));
        assertEquals(2, recentLogs.size());
    }
    
    @Test
    void testFindByUserId() {
        // 创建不同用户的调用日志
        ApiCallLog log1 = createTestApiLog("api1", 1000L, true);
        log1.setUserId("user1");
        
        ApiCallLog log2 = createTestApiLog("api2", 2000L, true);
        log2.setUserId("user1");
        
        ApiCallLog log3 = createTestApiLog("api3", 3000L, true);
        log3.setUserId("user2");
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试根据用户ID查找
        List<ApiCallLog> user1Logs = apiCallLogRepository.findByUserId("user1");
        assertEquals(2, user1Logs.size());
        
        List<ApiCallLog> user2Logs = apiCallLogRepository.findByUserId("user2");
        assertEquals(1, user2Logs.size());
    }
    
    @Test
    void testFindByResponseTimeMsGreaterThan() {
        // 创建不同响应时间的调用日志
        ApiCallLog fastLog = createTestApiLog("api1", 500L, true);
        ApiCallLog mediumLog = createTestApiLog("api2", 2000L, true);
        ApiCallLog slowLog = createTestApiLog("api3", 5000L, true);
        
        apiCallLogRepository.save(fastLog);
        apiCallLogRepository.save(mediumLog);
        apiCallLogRepository.save(slowLog);
        flushAndClear();
        
        // 测试查找响应时间超过阈值的日志
        List<ApiCallLog> slowLogs = apiCallLogRepository.findByResponseTimeMsGreaterThan(3000L);
        assertEquals(1, slowLogs.size());
        assertEquals(5000L, slowLogs.get(0).getResponseTimeMs());
    }
    
    @Test
    void testFindSlowQueries() {
        // 创建不同响应时间的调用日志
        ApiCallLog fastLog = createTestApiLog("api1", 1000L, true);
        ApiCallLog slowLog1 = createTestApiLog("api2", 4000L, true);
        ApiCallLog slowLog2 = createTestApiLog("api3", 5000L, true);
        
        apiCallLogRepository.save(fastLog);
        apiCallLogRepository.save(slowLog1);
        apiCallLogRepository.save(slowLog2);
        flushAndClear();
        
        // 测试查找慢查询（响应时间 > 3秒）
        List<ApiCallLog> slowQueries = apiCallLogRepository.findSlowQueries();
        assertEquals(2, slowQueries.size());
        // 验证按响应时间降序排列
        assertTrue(slowQueries.get(0).getResponseTimeMs() >= slowQueries.get(1).getResponseTimeMs());
    }
    
    @Test
    void testCountMethods() {
        // 创建测试数据
        ApiCallLog log1 = createTestApiLog("multimodal-api", 1000L, true);
        ApiCallLog log2 = createTestApiLog("multimodal-api", 2000L, false);
        ApiCallLog log3 = createTestApiLog("image-api", 1500L, true);
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试API调用总数统计
        long multimodalCount = apiCallLogRepository.countByApiName("multimodal-api");
        assertEquals(2, multimodalCount);
        
        // 测试成功调用数统计
        long successCount = apiCallLogRepository.countByApiNameAndSuccess("multimodal-api", true);
        assertEquals(1, successCount);
        
        long failCount = apiCallLogRepository.countByApiNameAndSuccess("multimodal-api", false);
        assertEquals(1, failCount);
    }
    
    @Test
    void testGetAverageResponseTime() {
        // 创建测试数据
        ApiCallLog log1 = createTestApiLog("test-api", 1000L, true);
        ApiCallLog log2 = createTestApiLog("test-api", 2000L, true);
        ApiCallLog log3 = createTestApiLog("test-api", 3000L, false); // 失败的调用不计入平均值
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试计算平均响应时间（只计算成功的调用）
        Double avgResponseTime = apiCallLogRepository.getAverageResponseTime("test-api");
        assertNotNull(avgResponseTime);
        assertEquals(1500.0, avgResponseTime, 0.1); // (1000 + 2000) / 2 = 1500
    }
    
    @Test
    void testGetSuccessRate() {
        // 创建测试数据
        ApiCallLog log1 = createTestApiLog("test-api", 1000L, true);
        ApiCallLog log2 = createTestApiLog("test-api", 2000L, true);
        ApiCallLog log3 = createTestApiLog("test-api", 3000L, false);
        
        apiCallLogRepository.save(log1);
        apiCallLogRepository.save(log2);
        apiCallLogRepository.save(log3);
        flushAndClear();
        
        // 测试计算成功率
        Double successRate = apiCallLogRepository.getSuccessRate("test-api");
        assertNotNull(successRate);
        assertEquals(66.67, successRate, 0.1); // 2/3 * 100 = 66.67%
    }
    
    @Test
    void testFindTop100ByOrderByCallTimeDesc() {
        // 创建多个调用日志
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            ApiCallLog log = createTestApiLog("api" + i, 1000L, true);
            log.setCallTime(now.minusMinutes(i));
            apiCallLogRepository.save(log);
        }
        flushAndClear();
        
        // 测试查找最近的调用记录
        List<ApiCallLog> recentLogs = apiCallLogRepository.findTop100ByOrderByCallTimeDesc();
        assertEquals(5, recentLogs.size());
        
        // 验证按时间降序排列
        for (int i = 0; i < recentLogs.size() - 1; i++) {
            assertTrue(recentLogs.get(i).getCallTime().isAfter(recentLogs.get(i + 1).getCallTime()) ||
                      recentLogs.get(i).getCallTime().equals(recentLogs.get(i + 1).getCallTime()));
        }
    }
    
    @Test
    void testDeleteApiCallLog() {
        // 创建测试日志
        ApiCallLog log = createTestApiLog("test-api", 1000L, true);
        ApiCallLog savedLog = apiCallLogRepository.save(log);
        flushAndClear();
        
        // 验证日志存在
        assertTrue(apiCallLogRepository.existsById(savedLog.getLogId()));
        
        // 删除日志
        apiCallLogRepository.deleteById(savedLog.getLogId());
        flushAndClear();
        
        // 验证日志已删除
        assertFalse(apiCallLogRepository.existsById(savedLog.getLogId()));
    }
    
    /**
     * 创建测试API调用日志的辅助方法
     */
    private ApiCallLog createTestApiLog(String apiName, Long responseTimeMs, Boolean success) {
        ApiCallLog log = new ApiCallLog(apiName, responseTimeMs, success);
        log.setStatusCode(success ? 200 : 500);
        log.setCallTime(LocalDateTime.now());
        log.setEndpoint("/api/test");
        log.setHttpMethod("POST");
        return log;
    }
}