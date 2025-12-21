package com.education.ai.repository;

import com.education.ai.model.AnalysisStatus;
import com.education.ai.model.GestureAnalysisRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 手势分析记录Repository单元测试
 * 测试手势分析记录数据访问层的CRUD操作和统计查询方法
 */
class GestureAnalysisRecordRepositoryTest extends RepositoryTestBase {
    
    @Autowired
    private GestureAnalysisRecordRepository gestureAnalysisRecordRepository;
    
    @Test
    void testSaveAndFindGestureAnalysisRecord() {
        // 创建测试手势分析记录
        GestureAnalysisRecord record = new GestureAnalysisRecord("analysis123", "user123", "{\"x\":100,\"y\":200}");
        record.setContentImage("base64_encoded_image");
        record.setExplanation("这是一个坐标点的分析");
        record.setConfidence(0.95);
        record.setStatus(AnalysisStatus.SUCCESS);
        record.setProcessingTimeMs(2500L);
        record.setSubjectArea("mathematics");
        record.setCreatedAt(LocalDateTime.now());
        
        // 保存记录
        GestureAnalysisRecord savedRecord = gestureAnalysisRecordRepository.save(record);
        flushAndClear();
        
        // 验证保存结果
        assertNotNull(savedRecord);
        assertEquals("analysis123", savedRecord.getAnalysisId());
        assertEquals("user123", savedRecord.getUserId());
        assertEquals("{\"x\":100,\"y\":200}", savedRecord.getGestureData());
        assertEquals("这是一个坐标点的分析", savedRecord.getExplanation());
        assertEquals(0.95, savedRecord.getConfidence());
        assertEquals(AnalysisStatus.SUCCESS, savedRecord.getStatus());
        assertEquals(2500L, savedRecord.getProcessingTimeMs());
        
        // 测试根据ID查找
        Optional<GestureAnalysisRecord> foundRecord = gestureAnalysisRecordRepository.findById("analysis123");
        assertTrue(foundRecord.isPresent());
        assertEquals("mathematics", foundRecord.get().getSubjectArea());
    }
    
    @Test
    void testFindByUserId() {
        // 创建不同用户的分析记录
        GestureAnalysisRecord record1 = createTestRecord("analysis1", "user1", "gesture1");
        GestureAnalysisRecord record2 = createTestRecord("analysis2", "user1", "gesture2");
        GestureAnalysisRecord record3 = createTestRecord("analysis3", "user2", "gesture3");
        
        gestureAnalysisRecordRepository.save(record1);
        gestureAnalysisRecordRepository.save(record2);
        gestureAnalysisRecordRepository.save(record3);
        flushAndClear();
        
        // 测试根据用户ID查找
        List<GestureAnalysisRecord> user1Records = gestureAnalysisRecordRepository.findByUserId("user1");
        assertEquals(2, user1Records.size());
        
        List<GestureAnalysisRecord> user2Records = gestureAnalysisRecordRepository.findByUserId("user2");
        assertEquals(1, user2Records.size());
        
        // 测试分页查询
        Page<GestureAnalysisRecord> page = gestureAnalysisRecordRepository.findByUserId("user1", PageRequest.of(0, 1));
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }
    
    @Test
    void testFindByStatus() {
        // 创建不同状态的分析记录
        GestureAnalysisRecord successRecord = createTestRecord("analysis1", "user1", "gesture1");
        successRecord.setStatus(AnalysisStatus.SUCCESS);
        
        GestureAnalysisRecord failedRecord = createTestRecord("analysis2", "user1", "gesture2");
        failedRecord.setStatus(AnalysisStatus.FAILED);
        
        GestureAnalysisRecord processingRecord = createTestRecord("analysis3", "user1", "gesture3");
        processingRecord.setStatus(AnalysisStatus.PROCESSING);
        
        gestureAnalysisRecordRepository.save(successRecord);
        gestureAnalysisRecordRepository.save(failedRecord);
        gestureAnalysisRecordRepository.save(processingRecord);
        flushAndClear();
        
        // 测试根据状态查找
        List<GestureAnalysisRecord> successRecords = gestureAnalysisRecordRepository.findByStatus(AnalysisStatus.SUCCESS);
        assertEquals(1, successRecords.size());
        assertEquals("analysis1", successRecords.get(0).getAnalysisId());
        
        List<GestureAnalysisRecord> failedRecords = gestureAnalysisRecordRepository.findByStatus(AnalysisStatus.FAILED);
        assertEquals(1, failedRecords.size());
        assertEquals("analysis2", failedRecords.get(0).getAnalysisId());
    }
    
    @Test
    void testFindByUserIdAndStatus() {
        // 创建测试数据
        GestureAnalysisRecord record1 = createTestRecord("analysis1", "user1", "gesture1");
        record1.setStatus(AnalysisStatus.SUCCESS);
        
        GestureAnalysisRecord record2 = createTestRecord("analysis2", "user1", "gesture2");
        record2.setStatus(AnalysisStatus.FAILED);
        
        GestureAnalysisRecord record3 = createTestRecord("analysis3", "user2", "gesture3");
        record3.setStatus(AnalysisStatus.SUCCESS);
        
        gestureAnalysisRecordRepository.save(record1);
        gestureAnalysisRecordRepository.save(record2);
        gestureAnalysisRecordRepository.save(record3);
        flushAndClear();
        
        // 测试根据用户ID和状态查找
        List<GestureAnalysisRecord> user1SuccessRecords = 
            gestureAnalysisRecordRepository.findByUserIdAndStatus("user1", AnalysisStatus.SUCCESS);
        assertEquals(1, user1SuccessRecords.size());
        assertEquals("analysis1", user1SuccessRecords.get(0).getAnalysisId());
        
        List<GestureAnalysisRecord> user1FailedRecords = 
            gestureAnalysisRecordRepository.findByUserIdAndStatus("user1", AnalysisStatus.FAILED);
        assertEquals(1, user1FailedRecords.size());
        assertEquals("analysis2", user1FailedRecords.get(0).getAnalysisId());
    }
    
    @Test
    void testFindByCreatedAtBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);
        
        // 创建不同时间的分析记录
        GestureAnalysisRecord oldRecord = createTestRecord("analysis1", "user1", "gesture1");
        oldRecord.setCreatedAt(twoHoursAgo);
        
        GestureAnalysisRecord recentRecord = createTestRecord("analysis2", "user1", "gesture2");
        recentRecord.setCreatedAt(oneHourAgo.plusMinutes(30));
        
        GestureAnalysisRecord newRecord = createTestRecord("analysis3", "user1", "gesture3");
        newRecord.setCreatedAt(now);
        
        gestureAnalysisRecordRepository.save(oldRecord);
        gestureAnalysisRecordRepository.save(recentRecord);
        gestureAnalysisRecordRepository.save(newRecord);
        flushAndClear();
        
        // 测试时间范围查询
        List<GestureAnalysisRecord> recentRecords = 
            gestureAnalysisRecordRepository.findByCreatedAtBetween(oneHourAgo, now);
        assertEquals(2, recentRecords.size());
    }
    
    @Test
    void testFindByUserIdAndCreatedAtBetween() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 创建测试数据
        GestureAnalysisRecord user1Record = createTestRecord("analysis1", "user1", "gesture1");
        user1Record.setCreatedAt(oneHourAgo.plusMinutes(30));
        
        GestureAnalysisRecord user2Record = createTestRecord("analysis2", "user2", "gesture2");
        user2Record.setCreatedAt(oneHourAgo.plusMinutes(30));
        
        GestureAnalysisRecord oldRecord = createTestRecord("analysis3", "user1", "gesture3");
        oldRecord.setCreatedAt(now.minusHours(2));
        
        gestureAnalysisRecordRepository.save(user1Record);
        gestureAnalysisRecordRepository.save(user2Record);
        gestureAnalysisRecordRepository.save(oldRecord);
        flushAndClear();
        
        // 测试根据用户ID和时间范围查找
        List<GestureAnalysisRecord> user1RecentRecords = 
            gestureAnalysisRecordRepository.findByUserIdAndCreatedAtBetween("user1", oneHourAgo, now);
        assertEquals(1, user1RecentRecords.size());
        assertEquals("analysis1", user1RecentRecords.get(0).getAnalysisId());
    }
    
    @Test
    void testFindByConfidenceGreaterThan() {
        // 创建不同置信度的记录
        GestureAnalysisRecord highConfidenceRecord = createTestRecord("analysis1", "user1", "gesture1");
        highConfidenceRecord.setConfidence(0.95);
        
        GestureAnalysisRecord mediumConfidenceRecord = createTestRecord("analysis2", "user1", "gesture2");
        mediumConfidenceRecord.setConfidence(0.75);
        
        GestureAnalysisRecord lowConfidenceRecord = createTestRecord("analysis3", "user1", "gesture3");
        lowConfidenceRecord.setConfidence(0.55);
        
        gestureAnalysisRecordRepository.save(highConfidenceRecord);
        gestureAnalysisRecordRepository.save(mediumConfidenceRecord);
        gestureAnalysisRecordRepository.save(lowConfidenceRecord);
        flushAndClear();
        
        // 测试查找高置信度记录
        List<GestureAnalysisRecord> highConfidenceRecords = 
            gestureAnalysisRecordRepository.findByConfidenceGreaterThan(0.8);
        assertEquals(1, highConfidenceRecords.size());
        assertEquals(0.95, highConfidenceRecords.get(0).getConfidence());
    }
    
    @Test
    void testFindByProcessingTimeMsGreaterThan() {
        // 创建不同处理时间的记录
        GestureAnalysisRecord fastRecord = createTestRecord("analysis1", "user1", "gesture1");
        fastRecord.setProcessingTimeMs(1000L);
        
        GestureAnalysisRecord mediumRecord = createTestRecord("analysis2", "user1", "gesture2");
        mediumRecord.setProcessingTimeMs(3000L);
        
        GestureAnalysisRecord slowRecord = createTestRecord("analysis3", "user1", "gesture3");
        slowRecord.setProcessingTimeMs(5000L);
        
        gestureAnalysisRecordRepository.save(fastRecord);
        gestureAnalysisRecordRepository.save(mediumRecord);
        gestureAnalysisRecordRepository.save(slowRecord);
        flushAndClear();
        
        // 测试查找处理时间超过阈值的记录
        List<GestureAnalysisRecord> slowRecords = 
            gestureAnalysisRecordRepository.findByProcessingTimeMsGreaterThan(4000L);
        assertEquals(1, slowRecords.size());
        assertEquals(5000L, slowRecords.get(0).getProcessingTimeMs());
    }
    
    @Test
    void testFindBySubjectArea() {
        // 创建不同学科领域的记录
        GestureAnalysisRecord mathRecord = createTestRecord("analysis1", "user1", "gesture1");
        mathRecord.setSubjectArea("mathematics");
        
        GestureAnalysisRecord physicsRecord = createTestRecord("analysis2", "user1", "gesture2");
        physicsRecord.setSubjectArea("physics");
        
        GestureAnalysisRecord chemRecord = createTestRecord("analysis3", "user1", "gesture3");
        chemRecord.setSubjectArea("chemistry");
        
        gestureAnalysisRecordRepository.save(mathRecord);
        gestureAnalysisRecordRepository.save(physicsRecord);
        gestureAnalysisRecordRepository.save(chemRecord);
        flushAndClear();
        
        // 测试根据学科领域查找
        List<GestureAnalysisRecord> mathRecords = 
            gestureAnalysisRecordRepository.findBySubjectArea("mathematics");
        assertEquals(1, mathRecords.size());
        assertEquals("analysis1", mathRecords.get(0).getAnalysisId());
    }
    
    @Test
    void testCountMethods() {
        // 创建测试数据
        GestureAnalysisRecord record1 = createTestRecord("analysis1", "user1", "gesture1");
        record1.setStatus(AnalysisStatus.SUCCESS);
        
        GestureAnalysisRecord record2 = createTestRecord("analysis2", "user1", "gesture2");
        record2.setStatus(AnalysisStatus.FAILED);
        
        GestureAnalysisRecord record3 = createTestRecord("analysis3", "user2", "gesture3");
        record3.setStatus(AnalysisStatus.SUCCESS);
        
        gestureAnalysisRecordRepository.save(record1);
        gestureAnalysisRecordRepository.save(record2);
        gestureAnalysisRecordRepository.save(record3);
        flushAndClear();
        
        // 测试用户记录计数
        long user1Count = gestureAnalysisRecordRepository.countByUserId("user1");
        assertEquals(2, user1Count);
        
        // 测试状态记录计数
        long successCount = gestureAnalysisRecordRepository.countByStatus(AnalysisStatus.SUCCESS);
        assertEquals(2, successCount);
        
        // 测试用户成功记录计数
        long user1SuccessCount = gestureAnalysisRecordRepository.countByUserIdAndStatus("user1", AnalysisStatus.SUCCESS);
        assertEquals(1, user1SuccessCount);
    }
    
    @Test
    void testFindTop10ByUserIdOrderByCreatedAtDesc() {
        // 创建多个记录
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            GestureAnalysisRecord record = createTestRecord("analysis" + i, "user1", "gesture" + i);
            record.setCreatedAt(now.minusMinutes(i));
            gestureAnalysisRecordRepository.save(record);
        }
        flushAndClear();
        
        // 测试查找用户最近的记录
        List<GestureAnalysisRecord> recentRecords = 
            gestureAnalysisRecordRepository.findTop10ByUserIdOrderByCreatedAtDesc("user1");
        assertEquals(5, recentRecords.size());
        
        // 验证按时间降序排列
        for (int i = 0; i < recentRecords.size() - 1; i++) {
            assertTrue(recentRecords.get(i).getCreatedAt().isAfter(recentRecords.get(i + 1).getCreatedAt()) ||
                      recentRecords.get(i).getCreatedAt().equals(recentRecords.get(i + 1).getCreatedAt()));
        }
    }
    
    @Test
    void testGetAverageProcessingTime() {
        // 创建测试数据
        GestureAnalysisRecord record1 = createTestRecord("analysis1", "user1", "gesture1");
        record1.setStatus(AnalysisStatus.SUCCESS);
        record1.setProcessingTimeMs(1000L);
        
        GestureAnalysisRecord record2 = createTestRecord("analysis2", "user1", "gesture2");
        record2.setStatus(AnalysisStatus.SUCCESS);
        record2.setProcessingTimeMs(2000L);
        
        GestureAnalysisRecord record3 = createTestRecord("analysis3", "user1", "gesture3");
        record3.setStatus(AnalysisStatus.FAILED);
        record3.setProcessingTimeMs(3000L); // 失败的记录不计入平均值
        
        gestureAnalysisRecordRepository.save(record1);
        gestureAnalysisRecordRepository.save(record2);
        gestureAnalysisRecordRepository.save(record3);
        flushAndClear();
        
        // 测试计算平均处理时间
        Double avgProcessingTime = gestureAnalysisRecordRepository.getAverageProcessingTime(AnalysisStatus.SUCCESS);
        assertNotNull(avgProcessingTime);
        assertEquals(1500.0, avgProcessingTime, 0.1); // (1000 + 2000) / 2 = 1500
    }
    
    @Test
    void testGetAverageConfidenceByUser() {
        // 创建测试数据
        GestureAnalysisRecord record1 = createTestRecord("analysis1", "user1", "gesture1");
        record1.setStatus(AnalysisStatus.SUCCESS);
        record1.setConfidence(0.8);
        
        GestureAnalysisRecord record2 = createTestRecord("analysis2", "user1", "gesture2");
        record2.setStatus(AnalysisStatus.SUCCESS);
        record2.setConfidence(0.9);
        
        GestureAnalysisRecord record3 = createTestRecord("analysis3", "user2", "gesture3");
        record3.setStatus(AnalysisStatus.SUCCESS);
        record3.setConfidence(0.7); // 不同用户的记录不计入
        
        gestureAnalysisRecordRepository.save(record1);
        gestureAnalysisRecordRepository.save(record2);
        gestureAnalysisRecordRepository.save(record3);
        flushAndClear();
        
        // 测试计算用户平均置信度
        Double avgConfidence = gestureAnalysisRecordRepository.getAverageConfidenceByUser("user1");
        assertNotNull(avgConfidence);
        assertEquals(0.85, avgConfidence, 0.01); // (0.8 + 0.9) / 2 = 0.85
    }
    
    @Test
    void testGetDailyAnalysisCount() {
        LocalDateTime today = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterday = today.minusDays(1);
        
        // 创建不同日期的记录
        GestureAnalysisRecord todayRecord1 = createTestRecord("analysis1", "user1", "gesture1");
        todayRecord1.setCreatedAt(today);
        
        GestureAnalysisRecord todayRecord2 = createTestRecord("analysis2", "user1", "gesture2");
        todayRecord2.setCreatedAt(today.plusHours(2));
        
        GestureAnalysisRecord yesterdayRecord = createTestRecord("analysis3", "user1", "gesture3");
        yesterdayRecord.setCreatedAt(yesterday);
        
        gestureAnalysisRecordRepository.save(todayRecord1);
        gestureAnalysisRecordRepository.save(todayRecord2);
        gestureAnalysisRecordRepository.save(yesterdayRecord);
        flushAndClear();
        
        // 测试每日分析记录数量统计
        List<Object[]> dailyStats = gestureAnalysisRecordRepository.getDailyAnalysisCount(yesterday.minusHours(1));
        assertNotNull(dailyStats);
        assertTrue(dailyStats.size() >= 1); // 至少有一天的数据
    }
    
    @Test
    void testFindHighConfidenceRecords() {
        // 创建不同置信度的记录
        GestureAnalysisRecord highRecord1 = createTestRecord("analysis1", "user1", "gesture1");
        highRecord1.setStatus(AnalysisStatus.SUCCESS);
        highRecord1.setConfidence(0.95);
        
        GestureAnalysisRecord highRecord2 = createTestRecord("analysis2", "user1", "gesture2");
        highRecord2.setStatus(AnalysisStatus.SUCCESS);
        highRecord2.setConfidence(0.88);
        
        GestureAnalysisRecord lowRecord = createTestRecord("analysis3", "user1", "gesture3");
        lowRecord.setStatus(AnalysisStatus.SUCCESS);
        lowRecord.setConfidence(0.75);
        
        gestureAnalysisRecordRepository.save(highRecord1);
        gestureAnalysisRecordRepository.save(highRecord2);
        gestureAnalysisRecordRepository.save(lowRecord);
        flushAndClear();
        
        // 测试查找高置信度记录
        List<GestureAnalysisRecord> highConfidenceRecords = 
            gestureAnalysisRecordRepository.findHighConfidenceRecords(0.85);
        assertEquals(2, highConfidenceRecords.size());
        
        // 验证按置信度降序排列
        assertTrue(highConfidenceRecords.get(0).getConfidence() >= highConfidenceRecords.get(1).getConfidence());
    }
    
    @Test
    void testFindRecordsForReprocessing() {
        // 创建不同状态的记录
        GestureAnalysisRecord successRecord = createTestRecord("analysis1", "user1", "gesture1");
        successRecord.setStatus(AnalysisStatus.SUCCESS);
        
        GestureAnalysisRecord failedRecord = createTestRecord("analysis2", "user1", "gesture2");
        failedRecord.setStatus(AnalysisStatus.FAILED);
        
        GestureAnalysisRecord timeoutRecord = createTestRecord("analysis3", "user1", "gesture3");
        timeoutRecord.setStatus(AnalysisStatus.TIMEOUT);
        
        gestureAnalysisRecordRepository.save(successRecord);
        gestureAnalysisRecordRepository.save(failedRecord);
        gestureAnalysisRecordRepository.save(timeoutRecord);
        flushAndClear();
        
        // 测试查找需要重新处理的记录
        List<GestureAnalysisRecord> reprocessRecords = 
            gestureAnalysisRecordRepository.findRecordsForReprocessing();
        assertEquals(2, reprocessRecords.size());
        
        // 验证返回的都是失败或超时状态
        for (GestureAnalysisRecord record : reprocessRecords) {
            assertTrue(record.getStatus() == AnalysisStatus.FAILED || 
                      record.getStatus() == AnalysisStatus.TIMEOUT);
        }
    }
    
    @Test
    void testDeleteGestureAnalysisRecord() {
        // 创建测试记录
        GestureAnalysisRecord record = createTestRecord("analysis123", "user123", "gesture123");
        gestureAnalysisRecordRepository.save(record);
        flushAndClear();
        
        // 验证记录存在
        assertTrue(gestureAnalysisRecordRepository.existsById("analysis123"));
        
        // 删除记录
        gestureAnalysisRecordRepository.deleteById("analysis123");
        flushAndClear();
        
        // 验证记录已删除
        assertFalse(gestureAnalysisRecordRepository.existsById("analysis123"));
    }
    
    /**
     * 创建测试手势分析记录的辅助方法
     */
    private GestureAnalysisRecord createTestRecord(String analysisId, String userId, String gestureData) {
        GestureAnalysisRecord record = new GestureAnalysisRecord(analysisId, userId, gestureData);
        record.setStatus(AnalysisStatus.SUCCESS);
        record.setConfidence(0.8);
        record.setProcessingTimeMs(2000L);
        record.setCreatedAt(LocalDateTime.now());
        record.setSubjectArea("mathematics");
        return record;
    }
}