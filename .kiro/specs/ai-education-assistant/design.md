# AI教育辅助应用后端系统设计文档

## 概述

AI教育辅助应用后端系统是一个基于Spring Boot 3.x和Maven构建的微服务架构系统，为移动端教育应用提供三大核心AI功能。系统采用分层架构设计，通过统一的API网关管理外部服务调用，实现高可用、高性能的教育辅助服务。

## 架构

### 整体架构

系统采用分层架构模式，包含以下层次：

```
┌─────────────────────────────────────────┐
│           移动端客户端 (Mobile Client)      │
└─────────────────┬───────────────────────┘
                  │ HTTP/WebSocket
┌─────────────────▼───────────────────────┐
│              API网关层                    │
│    (Spring Cloud Gateway/Zuul)          │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│              控制层 (Controller)          │
│  - GestureController                    │
│  - PhotoSearchController                │
│  - ExperimentController                 │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│              服务层 (Service)             │
│  - GestureAnalysisService               │
│  - PhotoSearchService                   │
│  - ExperimentDetectionService           │
│  - ApiIntegrationService                │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            数据访问层 (Repository)         │
│  - UserRepository                       │
│  - ExperimentRecordRepository           │
│  - ApiLogRepository                     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│              数据存储层                    │
│  - MySQL (持久化数据)                     │
│  - Redis (缓存和会话)                     │
└─────────────────────────────────────────┘
```

### 外部系统集成

```
┌─────────────────────────────────────────┐
│            外部API服务                    │
├─────────────────────────────────────────┤
│  - 多模态大模型API (Multimodal AI)        │
│  - 图像识别API (OCR Service)             │
│  - 题目解析API (Problem Solving)         │
│  - 教育合作伙伴API (Education Partner)    │
└─────────────────────────────────────────┘
```

## 组件和接口

### 核心组件

#### 1. 手势识别分析组件 (GestureAnalysisService)

```java
@Service
public class GestureAnalysisService {
    
    @Autowired
    private MultimodalApiClient multimodalApiClient;
    
    @Autowired
    private CacheService cacheService;
    
    public AnalysisResult analyzeGestureContent(GestureRequest request);
    public void validateGestureData(GestureData data);
    private String formatExplanation(ApiResponse response);
}
```

#### 2. 拍照搜题服务组件 (PhotoSearchService)

```java
@Service
public class PhotoSearchService {
    
    @Autowired
    private ImageProcessingService imageProcessingService;
    
    @Autowired
    private ProblemSolvingApiClient problemSolvingApiClient;
    
    @Autowired
    private VisualizationEngine visualizationEngine;
    
    public SearchResult searchByPhoto(MultipartFile image);
    public VisualizationContent generateVisualization(SolutionSteps steps);
    private boolean validateImageFormat(MultipartFile image);
}
```

#### 3. 实验检测服务组件 (ExperimentDetectionService)

```java
@Service
public class ExperimentDetectionService {
    
    @Autowired
    private EducationPartnerApiClient educationApiClient;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private ExperimentRecordRepository experimentRepository;
    
    public void startExperimentSession(String sessionId, ExperimentConfig config);
    public void processRealTimeData(String sessionId, ExperimentData data);
    public ExperimentReport generateReport(String sessionId);
    public void handleExperimentAlert(String sessionId, AlertType alertType);
}
```

#### 4. API集成管理组件 (ApiIntegrationService)

```java
@Service
public class ApiIntegrationService {
    
    @Autowired
    private List<ExternalApiClient> apiClients;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private ApiMetricsService metricsService;
    
    public <T> T callExternalApi(String apiName, ApiRequest request, Class<T> responseType);
    public void logApiCall(String apiName, long responseTime, boolean success);
    public ApiHealthStatus checkApiHealth(String apiName);
}
```

### RESTful API接口设计

#### 手势识别接口

```
POST /api/v1/gesture/analyze
Content-Type: application/json

Request:
{
    "gestureData": {
        "coordinates": [{"x": 100, "y": 200}],
        "timestamp": "2024-12-19T10:30:00Z"
    },
    "contentImage": "base64_encoded_image",
    "userId": "user123"
}

Response:
{
    "analysisId": "analysis_456",
    "explanation": "这是一个二次函数的图像...",
    "keyPoints": ["顶点", "对称轴", "开口方向"],
    "confidence": 0.95,
    "processingTime": 2.3
}
```

#### 拍照搜题接口

```
POST /api/v1/photo/search
Content-Type: multipart/form-data

Request:
- image: [image file]
- userId: user123
- subject: mathematics

Response:
{
    "searchId": "search_789",
    "problem": {
        "text": "求解方程 x² + 2x - 3 = 0",
        "type": "quadratic_equation"
    },
    "solution": {
        "steps": [...],
        "visualization": {
            "type": "interactive_graph",
            "data": {...}
        }
    }
}
```

#### 实验检测接口

```
WebSocket: /ws/experiment/{sessionId}

Message Types:
1. START_SESSION
2. EXPERIMENT_DATA
3. ALERT
4. END_SESSION

Example Message:
{
    "type": "EXPERIMENT_DATA",
    "sessionId": "exp_001",
    "data": {
        "temperature": 25.5,
        "pressure": 1.01,
        "timestamp": "2024-12-19T10:30:00Z"
    }
}
```

## 数据模型

### 用户模型

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private String userId;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean active;
}
```

### 手势分析记录模型

```java
@Entity
@Table(name = "gesture_analysis_records")
public class GestureAnalysisRecord {
    @Id
    private String analysisId;
    private String userId;
    private String gestureData;
    private String contentImage;
    private String explanation;
    private Double confidence;
    private LocalDateTime createdAt;
    private Long processingTimeMs;
}
```

### 实验会话模型

```java
@Entity
@Table(name = "experiment_sessions")
public class ExperimentSession {
    @Id
    private String sessionId;
    private String userId;
    private String experimentType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private String configData;
    private String reportData;
}
```

### API调用日志模型

```java
@Entity
@Table(name = "api_call_logs")
public class ApiCallLog {
    @Id
    private Long logId;
    private String apiName;
    private String requestData;
    private String responseData;
    private Integer statusCode;
    private Long responseTimeMs;
    private LocalDateTime callTime;
    private boolean success;
}
```

## 正确性属性

*属性是指在系统的所有有效执行中都应该保持为真的特征或行为——本质上是关于系统应该做什么的正式声明。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1: API调用正确性
*对于任何*有效的外部API请求（手势分析、图像识别、题目解析、实验检测），系统应该正确调用相应的外部API并处理响应
**验证需求: 需求 1.1, 2.1, 2.2, 3.1**

### 属性 2: 统一错误处理
*对于任何*无效输入或API调用失败的情况，系统应该返回标准化的错误响应并保持系统稳定状态
**验证需求: 需求 1.3, 1.4, 2.4**

### 属性 3: 数据处理一致性
*对于任何*从外部API获取的数据，系统应该按照预定义的格式进行处理和转换，确保输出格式的一致性
**验证需求: 需求 1.2, 2.3, 3.2**

### 属性 4: 实验会话完整性
*对于任何*实验检测会话，从开始到结束的整个生命周期应该被正确管理，包括数据保存和报告生成
**验证需求: 需求 3.4**

### 属性 5: 异常检测及时性
*对于任何*包含异常数据的实验流，系统应该立即检测并发送相应的警告信息
**验证需求: 需求 3.3**

### 属性 6: 监控和日志完整性
*对于任何*系统操作（API调用、用户操作），系统应该记录完整的日志信息包括时间戳、状态和相关参数
**验证需求: 需求 4.2, 6.5**

### 属性 7: 性能监控响应
*对于任何*超过预设阈值的API响应时间或失败率，系统应该触发相应的告警或熔断机制
**验证需求: 需求 4.3, 4.4**

### 属性 8: 安全数据处理
*对于任何*敏感教育数据，系统应该在存储和传输过程中进行加密处理
**验证需求: 需求 6.2**

### 属性 9: 认证授权一致性
*对于任何*用户请求，系统应该根据用户角色和权限进行一致的认证和授权验证
**验证需求: 需求 6.1**

### 属性 10: 数据备份恢复完整性
*对于任何*备份操作，恢复后的数据应该与原始数据保持完全一致
**验证需求: 需求 6.3**

### 属性 11: 限流保护有效性
*对于任何*高负载情况，系统应该正确启动限流机制保护核心服务的可用性
**验证需求: 需求 6.4**

## 错误处理

### 异常处理策略

系统采用分层异常处理机制：

1. **控制层异常处理**
   - 全局异常处理器 `@ControllerAdvice`
   - 统一错误响应格式
   - HTTP状态码标准化

2. **服务层异常处理**
   - 业务异常封装
   - 外部API调用异常处理
   - 重试机制和熔断器

3. **数据访问层异常处理**
   - 数据库连接异常
   - 数据完整性异常
   - 事务回滚机制

### 错误响应格式

```json
{
    "success": false,
    "errorCode": "GESTURE_ANALYSIS_FAILED",
    "message": "手势识别分析失败",
    "details": "多模态API调用超时",
    "timestamp": "2024-12-19T10:30:00Z",
    "requestId": "req_123456"
}
```

### 熔断器配置

```yaml
resilience4j:
  circuitbreaker:
    instances:
      multimodal-api:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
      image-recognition-api:
        failure-rate-threshold: 60
        wait-duration-in-open-state: 20s
        sliding-window-size: 8
```

## 测试策略

### 双重测试方法

系统采用单元测试和基于属性的测试相结合的综合测试策略：

- **单元测试**验证具体示例、边界情况和错误条件
- **基于属性的测试**验证应该在所有输入中保持的通用属性
- 两者结合提供全面覆盖：单元测试捕获具体错误，属性测试验证一般正确性

### 单元测试要求

单元测试通常涵盖：
- 演示正确行为的具体示例
- 组件之间的集成点
- 单元测试很有用，但避免编写过多。基于属性的测试负责处理大量输入的覆盖。

### 基于属性的测试要求

- 使用 **JUnit 5** 和 **jqwik** 作为基于属性的测试库
- 每个基于属性的测试配置为运行最少100次迭代
- 每个基于属性的测试必须使用注释明确引用设计文档中的正确性属性
- 测试标签格式：`**Feature: ai-education-assistant, Property {number}: {property_text}**`
- 每个正确性属性必须由单个基于属性的测试实现

### 测试框架和工具

- **单元测试**: JUnit 5, Mockito, Spring Boot Test
- **基于属性的测试**: jqwik
- **集成测试**: TestContainers, WireMock
- **性能测试**: JMeter, Spring Boot Actuator
- **API测试**: REST Assured

### 测试数据管理

- 使用TestContainers进行数据库集成测试
- 模拟外部API调用使用WireMock
- 测试数据工厂模式生成测试数据
- 测试环境隔离和清理

### 持续集成测试

- 代码提交触发自动化测试
- 测试覆盖率要求：行覆盖率 > 80%，分支覆盖率 > 70%
- 性能回归测试
- 安全漏洞扫描