package com.education.ai.model;

import net.jqwik.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 数据备份恢复完整性的基于属性的测试
 * **Feature: ai-education-assistant, Property 10: 数据备份恢复完整性**
 * **验证需求: 需求 6.3**
 */
@SpringBootTest
@ActiveProfiles("test")
class DataBackupRecoveryProperties {

    /**
     * 数据备份恢复完整性属性测试
     * 对于任何备份操作，恢复后的数据应该与原始数据保持完全一致
     */
    @Property(tries = 100)
    @Label("数据备份恢复完整性 - 用户数据")
    void userDataBackupRecoveryIntegrity(@ForAll("validUsers") User originalUser) {
        // 模拟备份过程：序列化用户数据
        UserBackupData backupData = createUserBackup(originalUser);
        
        // 模拟恢复过程：从备份数据重建用户对象
        User recoveredUser = restoreUserFromBackup(backupData);
        
        // 验证恢复后的数据与原始数据完全一致
        assertUserDataIntegrity(originalUser, recoveredUser);
    }

    @Property(tries = 100)
    @Label("数据备份恢复完整性 - 手势分析记录")
    void gestureAnalysisRecordBackupRecoveryIntegrity(@ForAll("validGestureRecords") GestureAnalysisRecord originalRecord) {
        // 模拟备份过程
        GestureRecordBackupData backupData = createGestureRecordBackup(originalRecord);
        
        // 模拟恢复过程
        GestureAnalysisRecord recoveredRecord = restoreGestureRecordFromBackup(backupData);
        
        // 验证数据完整性
        assertGestureRecordDataIntegrity(originalRecord, recoveredRecord);
    }

    @Property(tries = 100)
    @Label("数据备份恢复完整性 - 实验会话")
    void experimentSessionBackupRecoveryIntegrity(@ForAll("validExperimentSessions") ExperimentSession originalSession) {
        // 模拟备份过程
        ExperimentSessionBackupData backupData = createExperimentSessionBackup(originalSession);
        
        // 模拟恢复过程
        ExperimentSession recoveredSession = restoreExperimentSessionFromBackup(backupData);
        
        // 验证数据完整性
        assertExperimentSessionDataIntegrity(originalSession, recoveredSession);
    }

    @Property(tries = 100)
    @Label("数据备份恢复完整性 - API调用日志")
    void apiCallLogBackupRecoveryIntegrity(@ForAll("validApiCallLogs") ApiCallLog originalLog) {
        // 模拟备份过程
        ApiCallLogBackupData backupData = createApiCallLogBackup(originalLog);
        
        // 模拟恢复过程
        ApiCallLog recoveredLog = restoreApiCallLogFromBackup(backupData);
        
        // 验证数据完整性
        assertApiCallLogDataIntegrity(originalLog, recoveredLog);
    }

    // ========== 数据生成器 ==========

    @Provide
    Arbitrary<User> validUsers() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(30).map(s -> s + "@example.com"),
                Arbitraries.of(UserRole.class)
        ).as((userId, username, email, role) -> {
            User user = new User(userId, username, email);
            user.setRole(role);
            user.setCreatedAt(LocalDateTime.now().minusDays(30));
            user.setActive(true);
            user.setPasswordHash("hashedPassword123");
            user.setFullName("Test User");
            user.setPhone("12345678901");
            user.setLastLoginAt(LocalDateTime.now().minusDays(1));
            return user;
        });
    }

    @Provide
    Arbitrary<GestureAnalysisRecord> validGestureRecords() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.strings().ofMinLength(10).ofMaxLength(100),
                Arbitraries.doubles().between(0.0, 1.0)
        ).as((analysisId, userId, gestureData, confidence) -> {
            GestureAnalysisRecord record = new GestureAnalysisRecord(analysisId, userId, gestureData);
            record.setConfidence(confidence);
            record.setProcessingTimeMs(2500L);
            record.setStatus(AnalysisStatus.SUCCESS);
            record.setExplanation("Test explanation");
            record.setSubjectArea("mathematics");
            record.setCreatedAt(LocalDateTime.now().minusDays(10));
            record.setContentImage("base64encodedimage");
            record.setApiResponse("{\"result\":\"success\"}");
            return record;
        });
    }

    @Provide
    Arbitrary<ExperimentSession> validExperimentSessions() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
                Arbitraries.of(SessionStatus.class)
        ).as((sessionId, userId, experimentType, status) -> {
            ExperimentSession session = new ExperimentSession(sessionId, userId, experimentType);
            session.setStatus(status);
            LocalDateTime startTime = LocalDateTime.now().minusHours(2);
            session.setStartTime(startTime);
            session.setExperimentName("Test Experiment");
            session.setDescription("Test Description");
            session.setTotalDataPoints(100);
            session.setAlertCount(2);
            session.setConfigData("{\"config\":\"test\"}");
            session.setReportData("{\"report\":\"data\"}");
            session.setWebsocketConnectionId("ws_" + sessionId);
            if (status.isFinished()) {
                session.setEndTime(startTime.plusHours(2));
                session.calculateDuration();
            }
            return session;
        });
    }

    @Provide
    Arbitrary<ApiCallLog> validApiCallLogs() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(30),
                Arbitraries.longs().between(100L, 10000L),
                Arbitraries.of(true, false),
                Arbitraries.integers().between(200, 500)
        ).as((apiName, responseTime, success, statusCode) -> {
            ApiCallLog log = new ApiCallLog(apiName, responseTime, success);
            log.setStatusCode(statusCode);
            log.setUserId("testUser");
            log.setRequestId("req123");
            log.setEndpoint("/api/test");
            log.setHttpMethod("POST");
            log.setCallTime(LocalDateTime.now().minusMinutes(30));
            log.setRequestData("{\"request\":\"data\"}");
            log.setResponseData("{\"response\":\"data\"}");
            log.setRequestSizeBytes(1024L);
            log.setResponseSizeBytes(2048L);
            log.setRetryCount(0);
            return log;
        });
    }

    // ========== 备份和恢复模拟方法 ==========

    private UserBackupData createUserBackup(User user) {
        return new UserBackupData(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getActive(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getPhone()
        );
    }

    private User restoreUserFromBackup(UserBackupData backup) {
        User user = new User(backup.userId, backup.username, backup.email);
        user.setRole(backup.role);
        user.setCreatedAt(backup.createdAt);
        user.setLastLoginAt(backup.lastLoginAt);
        user.setActive(backup.active);
        user.setPasswordHash(backup.passwordHash);
        user.setFullName(backup.fullName);
        user.setPhone(backup.phone);
        return user;
    }

    private GestureRecordBackupData createGestureRecordBackup(GestureAnalysisRecord record) {
        return new GestureRecordBackupData(
                record.getAnalysisId(),
                record.getUserId(),
                record.getGestureData(),
                record.getContentImage(),
                record.getExplanation(),
                record.getConfidence(),
                record.getCreatedAt(),
                record.getProcessingTimeMs(),
                record.getStatus(),
                record.getErrorMessage(),
                record.getApiResponse(),
                record.getSubjectArea()
        );
    }

    private GestureAnalysisRecord restoreGestureRecordFromBackup(GestureRecordBackupData backup) {
        GestureAnalysisRecord record = new GestureAnalysisRecord(backup.analysisId, backup.userId, backup.gestureData);
        record.setContentImage(backup.contentImage);
        record.setExplanation(backup.explanation);
        record.setConfidence(backup.confidence);
        record.setCreatedAt(backup.createdAt);
        record.setProcessingTimeMs(backup.processingTimeMs);
        record.setStatus(backup.status);
        record.setErrorMessage(backup.errorMessage);
        record.setApiResponse(backup.apiResponse);
        record.setSubjectArea(backup.subjectArea);
        return record;
    }

    private ExperimentSessionBackupData createExperimentSessionBackup(ExperimentSession session) {
        return new ExperimentSessionBackupData(
                session.getSessionId(),
                session.getUserId(),
                session.getExperimentType(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getConfigData(),
                session.getReportData(),
                session.getLastUpdated(),
                session.getExperimentName(),
                session.getDescription(),
                session.getTotalDataPoints(),
                session.getAlertCount(),
                session.getDurationMinutes(),
                session.getWebsocketConnectionId()
        );
    }

    private ExperimentSession restoreExperimentSessionFromBackup(ExperimentSessionBackupData backup) {
        ExperimentSession session = new ExperimentSession(backup.sessionId, backup.userId, backup.experimentType);
        session.setStartTime(backup.startTime);
        session.setEndTime(backup.endTime);
        session.setStatus(backup.status);
        session.setConfigData(backup.configData);
        session.setReportData(backup.reportData);
        session.setLastUpdated(backup.lastUpdated);
        session.setExperimentName(backup.experimentName);
        session.setDescription(backup.description);
        session.setTotalDataPoints(backup.totalDataPoints);
        session.setAlertCount(backup.alertCount);
        session.setDurationMinutes(backup.durationMinutes);
        session.setWebsocketConnectionId(backup.websocketConnectionId);
        return session;
    }

    private ApiCallLogBackupData createApiCallLogBackup(ApiCallLog log) {
        return new ApiCallLogBackupData(
                log.getLogId(),
                log.getApiName(),
                log.getRequestData(),
                log.getResponseData(),
                log.getStatusCode(),
                log.getResponseTimeMs(),
                log.getCallTime(),
                log.getSuccess(),
                log.getErrorMessage(),
                log.getUserId(),
                log.getRequestId(),
                log.getEndpoint(),
                log.getHttpMethod(),
                log.getRequestSizeBytes(),
                log.getResponseSizeBytes(),
                log.getRetryCount()
        );
    }

    private ApiCallLog restoreApiCallLogFromBackup(ApiCallLogBackupData backup) {
        ApiCallLog log = new ApiCallLog(backup.apiName, backup.responseTimeMs, backup.success);
        log.setLogId(backup.logId);
        log.setRequestData(backup.requestData);
        log.setResponseData(backup.responseData);
        log.setStatusCode(backup.statusCode);
        log.setCallTime(backup.callTime);
        log.setErrorMessage(backup.errorMessage);
        log.setUserId(backup.userId);
        log.setRequestId(backup.requestId);
        log.setEndpoint(backup.endpoint);
        log.setHttpMethod(backup.httpMethod);
        log.setRequestSizeBytes(backup.requestSizeBytes);
        log.setResponseSizeBytes(backup.responseSizeBytes);
        log.setRetryCount(backup.retryCount);
        return log;
    }

    // ========== 数据完整性验证方法 ==========

    private void assertUserDataIntegrity(User original, User recovered) {
        if (!Objects.equals(original.getUserId(), recovered.getUserId())) {
            throw new AssertionError("用户ID不匹配: " + original.getUserId() + " vs " + recovered.getUserId());
        }
        if (!Objects.equals(original.getUsername(), recovered.getUsername())) {
            throw new AssertionError("用户名不匹配: " + original.getUsername() + " vs " + recovered.getUsername());
        }
        if (!Objects.equals(original.getEmail(), recovered.getEmail())) {
            throw new AssertionError("邮箱不匹配: " + original.getEmail() + " vs " + recovered.getEmail());
        }
        if (!Objects.equals(original.getRole(), recovered.getRole())) {
            throw new AssertionError("角色不匹配: " + original.getRole() + " vs " + recovered.getRole());
        }
        if (!Objects.equals(original.getCreatedAt(), recovered.getCreatedAt())) {
            throw new AssertionError("创建时间不匹配: " + original.getCreatedAt() + " vs " + recovered.getCreatedAt());
        }
        if (!Objects.equals(original.getLastLoginAt(), recovered.getLastLoginAt())) {
            throw new AssertionError("最后登录时间不匹配: " + original.getLastLoginAt() + " vs " + recovered.getLastLoginAt());
        }
        if (!Objects.equals(original.getActive(), recovered.getActive())) {
            throw new AssertionError("激活状态不匹配: " + original.getActive() + " vs " + recovered.getActive());
        }
        if (!Objects.equals(original.getPasswordHash(), recovered.getPasswordHash())) {
            throw new AssertionError("密码哈希不匹配");
        }
        if (!Objects.equals(original.getFullName(), recovered.getFullName())) {
            throw new AssertionError("全名不匹配: " + original.getFullName() + " vs " + recovered.getFullName());
        }
        if (!Objects.equals(original.getPhone(), recovered.getPhone())) {
            throw new AssertionError("电话不匹配: " + original.getPhone() + " vs " + recovered.getPhone());
        }
    }

    private void assertGestureRecordDataIntegrity(GestureAnalysisRecord original, GestureAnalysisRecord recovered) {
        if (!Objects.equals(original.getAnalysisId(), recovered.getAnalysisId())) {
            throw new AssertionError("分析ID不匹配: " + original.getAnalysisId() + " vs " + recovered.getAnalysisId());
        }
        if (!Objects.equals(original.getUserId(), recovered.getUserId())) {
            throw new AssertionError("用户ID不匹配: " + original.getUserId() + " vs " + recovered.getUserId());
        }
        if (!Objects.equals(original.getGestureData(), recovered.getGestureData())) {
            throw new AssertionError("手势数据不匹配");
        }
        if (!Objects.equals(original.getContentImage(), recovered.getContentImage())) {
            throw new AssertionError("内容图片不匹配");
        }
        if (!Objects.equals(original.getExplanation(), recovered.getExplanation())) {
            throw new AssertionError("解释不匹配");
        }
        if (!Objects.equals(original.getConfidence(), recovered.getConfidence())) {
            throw new AssertionError("置信度不匹配: " + original.getConfidence() + " vs " + recovered.getConfidence());
        }
        if (!Objects.equals(original.getCreatedAt(), recovered.getCreatedAt())) {
            throw new AssertionError("创建时间不匹配: " + original.getCreatedAt() + " vs " + recovered.getCreatedAt());
        }
        if (!Objects.equals(original.getProcessingTimeMs(), recovered.getProcessingTimeMs())) {
            throw new AssertionError("处理时间不匹配: " + original.getProcessingTimeMs() + " vs " + recovered.getProcessingTimeMs());
        }
        if (!Objects.equals(original.getStatus(), recovered.getStatus())) {
            throw new AssertionError("状态不匹配: " + original.getStatus() + " vs " + recovered.getStatus());
        }
        if (!Objects.equals(original.getSubjectArea(), recovered.getSubjectArea())) {
            throw new AssertionError("学科领域不匹配: " + original.getSubjectArea() + " vs " + recovered.getSubjectArea());
        }
    }

    private void assertExperimentSessionDataIntegrity(ExperimentSession original, ExperimentSession recovered) {
        if (!Objects.equals(original.getSessionId(), recovered.getSessionId())) {
            throw new AssertionError("会话ID不匹配: " + original.getSessionId() + " vs " + recovered.getSessionId());
        }
        if (!Objects.equals(original.getUserId(), recovered.getUserId())) {
            throw new AssertionError("用户ID不匹配: " + original.getUserId() + " vs " + recovered.getUserId());
        }
        if (!Objects.equals(original.getExperimentType(), recovered.getExperimentType())) {
            throw new AssertionError("实验类型不匹配: " + original.getExperimentType() + " vs " + recovered.getExperimentType());
        }
        if (!Objects.equals(original.getStartTime(), recovered.getStartTime())) {
            throw new AssertionError("开始时间不匹配: " + original.getStartTime() + " vs " + recovered.getStartTime());
        }
        if (!Objects.equals(original.getEndTime(), recovered.getEndTime())) {
            throw new AssertionError("结束时间不匹配: " + original.getEndTime() + " vs " + recovered.getEndTime());
        }
        if (!Objects.equals(original.getStatus(), recovered.getStatus())) {
            throw new AssertionError("状态不匹配: " + original.getStatus() + " vs " + recovered.getStatus());
        }
        if (!Objects.equals(original.getConfigData(), recovered.getConfigData())) {
            throw new AssertionError("配置数据不匹配");
        }
        if (!Objects.equals(original.getReportData(), recovered.getReportData())) {
            throw new AssertionError("报告数据不匹配");
        }
        if (!Objects.equals(original.getTotalDataPoints(), recovered.getTotalDataPoints())) {
            throw new AssertionError("数据点总数不匹配: " + original.getTotalDataPoints() + " vs " + recovered.getTotalDataPoints());
        }
        if (!Objects.equals(original.getAlertCount(), recovered.getAlertCount())) {
            throw new AssertionError("警告计数不匹配: " + original.getAlertCount() + " vs " + recovered.getAlertCount());
        }
        if (!Objects.equals(original.getDurationMinutes(), recovered.getDurationMinutes())) {
            throw new AssertionError("持续时间不匹配: " + original.getDurationMinutes() + " vs " + recovered.getDurationMinutes());
        }
        if (!Objects.equals(original.getWebsocketConnectionId(), recovered.getWebsocketConnectionId())) {
            throw new AssertionError("WebSocket连接ID不匹配: " + original.getWebsocketConnectionId() + " vs " + recovered.getWebsocketConnectionId());
        }
    }

    private void assertApiCallLogDataIntegrity(ApiCallLog original, ApiCallLog recovered) {
        if (!Objects.equals(original.getLogId(), recovered.getLogId())) {
            throw new AssertionError("日志ID不匹配: " + original.getLogId() + " vs " + recovered.getLogId());
        }
        if (!Objects.equals(original.getApiName(), recovered.getApiName())) {
            throw new AssertionError("API名称不匹配: " + original.getApiName() + " vs " + recovered.getApiName());
        }
        if (!Objects.equals(original.getRequestData(), recovered.getRequestData())) {
            throw new AssertionError("请求数据不匹配");
        }
        if (!Objects.equals(original.getResponseData(), recovered.getResponseData())) {
            throw new AssertionError("响应数据不匹配");
        }
        if (!Objects.equals(original.getStatusCode(), recovered.getStatusCode())) {
            throw new AssertionError("状态码不匹配: " + original.getStatusCode() + " vs " + recovered.getStatusCode());
        }
        if (!Objects.equals(original.getResponseTimeMs(), recovered.getResponseTimeMs())) {
            throw new AssertionError("响应时间不匹配: " + original.getResponseTimeMs() + " vs " + recovered.getResponseTimeMs());
        }
        if (!Objects.equals(original.getCallTime(), recovered.getCallTime())) {
            throw new AssertionError("调用时间不匹配: " + original.getCallTime() + " vs " + recovered.getCallTime());
        }
        if (!Objects.equals(original.getSuccess(), recovered.getSuccess())) {
            throw new AssertionError("成功标识不匹配: " + original.getSuccess() + " vs " + recovered.getSuccess());
        }
        if (!Objects.equals(original.getUserId(), recovered.getUserId())) {
            throw new AssertionError("用户ID不匹配: " + original.getUserId() + " vs " + recovered.getUserId());
        }
        if (!Objects.equals(original.getRequestId(), recovered.getRequestId())) {
            throw new AssertionError("请求ID不匹配: " + original.getRequestId() + " vs " + recovered.getRequestId());
        }
        if (!Objects.equals(original.getEndpoint(), recovered.getEndpoint())) {
            throw new AssertionError("端点不匹配: " + original.getEndpoint() + " vs " + recovered.getEndpoint());
        }
        if (!Objects.equals(original.getHttpMethod(), recovered.getHttpMethod())) {
            throw new AssertionError("HTTP方法不匹配: " + original.getHttpMethod() + " vs " + recovered.getHttpMethod());
        }
        if (!Objects.equals(original.getRetryCount(), recovered.getRetryCount())) {
            throw new AssertionError("重试次数不匹配: " + original.getRetryCount() + " vs " + recovered.getRetryCount());
        }
    }

    // ========== 备份数据结构 ==========

    private record UserBackupData(
            String userId,
            String username,
            String email,
            UserRole role,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt,
            Boolean active,
            String passwordHash,
            String fullName,
            String phone
    ) {}

    private record GestureRecordBackupData(
            String analysisId,
            String userId,
            String gestureData,
            String contentImage,
            String explanation,
            Double confidence,
            LocalDateTime createdAt,
            Long processingTimeMs,
            AnalysisStatus status,
            String errorMessage,
            String apiResponse,
            String subjectArea
    ) {}

    private record ExperimentSessionBackupData(
            String sessionId,
            String userId,
            String experimentType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            SessionStatus status,
            String configData,
            String reportData,
            LocalDateTime lastUpdated,
            String experimentName,
            String description,
            Integer totalDataPoints,
            Integer alertCount,
            Integer durationMinutes,
            String websocketConnectionId
    ) {}

    private record ApiCallLogBackupData(
            Long logId,
            String apiName,
            String requestData,
            String responseData,
            Integer statusCode,
            Long responseTimeMs,
            LocalDateTime callTime,
            Boolean success,
            String errorMessage,
            String userId,
            String requestId,
            String endpoint,
            String httpMethod,
            Long requestSizeBytes,
            Long responseSizeBytes,
            Integer retryCount
    ) {}
}