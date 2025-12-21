# AI教育辅助应用后端系统

基于Spring Boot 3.x和Maven构建的AI教育辅助应用后端系统，为移动端教育应用提供三大核心功能：

1. **手势识别内容分析** - 通过手势指向内容获得AI分析讲解
2. **拍照搜题可视化演示** - 拍照上传题目获得带可视化演示的解答
3. **实时检测实验辅助** - 实时检测实验过程并提供辅助指导

## 技术栈

- **框架**: Spring Boot 3.2.0
- **构建工具**: Maven
- **数据库**: MySQL 8.0 + Redis
- **安全**: Spring Security + JWT
- **测试**: JUnit 5 + jqwik (Property-based Testing)
- **监控**: Spring Boot Actuator + Resilience4j

## 项目结构

```
src/
├── main/
│   ├── java/com/education/ai/
│   │   ├── AiEducationAssistantApplication.java  # 主应用类
│   │   ├── client/                               # 外部API客户端
│   │   ├── config/                               # 配置类
│   │   │   ├── SecurityConfig.java               # 安全配置
│   │   │   ├── RedisConfig.java                  # Redis配置
│   │   │   ├── WebSocketConfig.java              # WebSocket配置
│   │   │   └── ApiClientConfig.java              # API客户端配置
│   │   ├── controller/                           # 控制层
│   │   ├── service/                              # 服务层
│   │   ├── repository/                           # 数据访问层
│   │   └── model/                                # 数据模型
│   └── resources/
│       └── application.yml                       # 应用配置
└── test/
    ├── java/com/education/ai/                    # 测试代码
    └── resources/
        └── application-test.yml                  # 测试配置
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 运行应用

1. 克隆项目
2. 配置数据库连接（application.yml）
3. 启动Redis服务
4. 运行应用：
   ```bash
   mvn spring-boot:run
   ```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=AiEducationAssistantApplicationTests
```

## API文档

应用启动后，可通过以下地址访问：

- 健康检查: http://localhost:8080/api/actuator/health
- 应用信息: http://localhost:8080/api/actuator/info

## 配置说明

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_education
    username: your_username
    password: your_password
```

### 外部API配置

```yaml
external-api:
  multimodal:
    base-url: https://api.multimodal.example.com
    api-key: your-api-key
```

## 开发指南

### 添加新功能

1. 在相应的包中创建服务类
2. 实现对应的控制器
3. 添加数据模型（如需要）
4. 编写单元测试和属性测试
5. 更新API文档

### 测试策略

项目采用双重测试方法：
- **单元测试**: 验证具体示例和边界情况
- **基于属性的测试**: 验证通用属性和正确性保证

## 许可证

[MIT License](LICENSE)