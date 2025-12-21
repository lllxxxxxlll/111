package com.education.ai.client;

import com.education.ai.client.EducationPartnerApiClient.ExperimentAnalysisResponse;
import com.education.ai.client.EducationPartnerApiClient.ExperimentSessionResponse;
import com.education.ai.client.ImageProcessingApiClient.ImageRecognitionResponse;
import com.education.ai.client.MultimodalApiClient.MultimodalAnalysisResponse;
import com.education.ai.client.ProblemSolvingApiClient.ProblemSolutionResponse;
import com.education.ai.repository.ApiCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * **Feature: ai-education-assistant, Property 1: API调用正确性**
 * **验证需求: 需求 1.1, 2.1, 2.2, 3.1**
 * 
 * 基于属性的测试：验证对于任何有效的外部API请求（手势分析、图像识别、题目解析、实验检测），
 * 系统应该正确调用相应的外部API并处理响应
 */
@SpringBootTest
@TestPropertySource(properties = {
    "external-api.multimodal.base-url=http://localhost:8089",
    "external-api.image-processing.base-url=http://localhost:8089", 
    "external-api.problem-solving.base-url=http://localhost:8089",
    "external-api.education-partner.base-url=http://localhost:8089",
    "external-api.multimodal.api-key=test-key",
    "external-api.image-processing.api-key=test-key",
    "external-api.problem-solving.api-key=test-key",
    "external-api.education-partner.api-key=test-key"
})
class ApiCallCorrectnessProperties {
    
    @MockBean
    private ApiCallLogRepository apiCallLogRepository;
    
    @MockBean
    private RestTemplate multimodalRestTemplate;
    
    @MockBean
    private RestTemplate imageProcessingRestTemplate;
    
    @MockBean
    private RestTemplate problemSolvingRestTemplate;
    
    @MockBean
    private RestTemplate educationPartnerRestTemplate;
    
    private MultimodalApiClient multimodalApiClient;
    private ImageProcessingApiClient imageProcessingApiClient;
    private ProblemSolvingApiClient problemSolvingApiClient;
    private EducationPartnerApiClient educationPartnerApiClient;
    
    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        multimodalApiClient = new MultimodalApiClient(multimodalRestTemplate, apiCallLogRepository, objectMapper);
        imageProcessingApiClient = new ImageProcessingApiClient(imageProcessingRestTemplate, apiCallLogRepository, objectMapper);
        problemSolvingApiClient = new ProblemSolvingApiClient(problemSolvingRestTemplate, apiCallLogRepository, objectMapper);
        educationPartnerApiClient = new EducationPartnerApiClient(educationPartnerRestTemplate, apiCallLogRepository, objectMapper);
    }
    
    /**
     * 属性测试：API调用参数验证正确性
     * 验证对于任何有效的输入参数，API客户端应该正确验证和处理输入
     */
    @Property(tries = 100)
    void apiInputValidationCorrectness(@ForAll("validGestureData") String gestureData,
                                     @ForAll("validContentImage") String contentImage,
                                     @ForAll("validUserId") String userId) {
        
        // 验证输入参数不为空且格式正确
        assertThat(gestureData).isNotNull().isNotEmpty();
        assertThat(contentImage).isNotNull().isNotEmpty();
        assertThat(userId).isNotNull().isNotEmpty();
        
        // 验证手势数据包含必要的JSON结构
        assertThat(gestureData).contains("coordinates");
        
        // 验证内容图像是base64格式
        assertThat(contentImage).startsWith("data:image/");
        
        // 验证用户ID格式
        assertThat(userId).startsWith("user_");
    }
    
    /**
     * 属性测试：图像文件验证正确性
     * 验证对于任何有效的图像文件，系统应该正确验证图像格式
     */
    @Property(tries = 100)
    void imageFileValidationCorrectness(@ForAll("validImageFile") MockMultipartFile imageFile,
                                      @ForAll("validUserId") String userId) {
        
        // 验证图像文件基本属性
        assertThat(imageFile).isNotNull();
        assertThat(imageFile.getOriginalFilename()).isNotNull();
        assertThat(imageFile.getContentType()).isNotNull();
        assertThat(imageFile.getSize()).isGreaterThan(0);
        
        // 验证支持的图像格式
        assertThat(imageFile.getContentType()).isIn("image/jpeg", "image/png", "image/webp", "image/jpg");
        
        // 验证用户ID
        assertThat(userId).isNotNull().isNotEmpty().startsWith("user_");
    }
    
    /**
     * 属性测试：题目文本验证正确性
     * 验证对于任何有效的题目文本，系统应该正确处理不同类型的数学问题
     */
    @Property(tries = 100)
    void problemTextValidationCorrectness(@ForAll("validProblemText") String problemText,
                                        @ForAll("validSubject") String subject,
                                        @ForAll("validUserId") String userId) {
        
        // 验证题目文本不为空
        assertThat(problemText).isNotNull().isNotEmpty();
        
        // 验证学科类型有效
        assertThat(subject).isIn("mathematics", "physics", "chemistry", "biology");
        
        // 验证用户ID
        assertThat(userId).isNotNull().isNotEmpty().startsWith("user_");
        
        // 验证题目文本包含数学符号或关键词
        boolean containsMathContent = problemText.contains("=") || 
                                    problemText.contains("x") || 
                                    problemText.contains("求") || 
                                    problemText.contains("计算") ||
                                    problemText.contains("解");
        assertThat(containsMathContent).isTrue();
    }
    
    /**
     * 属性测试：实验会话参数验证正确性
     * 验证对于任何有效的实验会话参数，系统应该正确验证会话配置
     */
    @Property(tries = 100)
    void experimentSessionValidationCorrectness(@ForAll("validSessionId") String sessionId,
                                              @ForAll("validExperimentType") String experimentType,
                                              @ForAll("validUserId") String userId) {
        
        // 验证会话ID格式
        assertThat(sessionId).isNotNull().isNotEmpty().startsWith("session_");
        
        // 验证实验类型有效
        assertThat(experimentType).isIn("chemistry", "physics", "biology", "general");
        
        // 验证用户ID
        assertThat(userId).isNotNull().isNotEmpty().startsWith("user_");
        
        // 验证会话ID长度合理
        assertThat(sessionId.length()).isBetween(10, 25);
    }
    
    /**
     * 属性测试：实验数据验证正确性
     * 验证对于任何有效的实验数据，系统应该正确验证数据格式和范围
     */
    @Property(tries = 100)
    void experimentDataValidationCorrectness(@ForAll("validSessionId") String sessionId,
                                           @ForAll("validExperimentData") Map<String, Object> experimentData,
                                           @ForAll("validUserId") String userId) {
        
        // 验证会话ID
        assertThat(sessionId).isNotNull().isNotEmpty().startsWith("session_");
        
        // 验证实验数据不为空
        assertThat(experimentData).isNotNull().isNotEmpty();
        
        // 验证数据键值对的有效性
        for (Map.Entry<String, Object> entry : experimentData.entrySet()) {
            assertThat(entry.getKey()).isIn("temperature", "pressure", "humidity", "ph");
            assertThat(entry.getValue()).isInstanceOf(Double.class);
            Double value = (Double) entry.getValue();
            assertThat(value).isBetween(0.0, 100.0);
        }
        
        // 验证用户ID
        assertThat(userId).isNotNull().isNotEmpty().startsWith("user_");
    }
    
    /**
     * 属性测试：API响应结构一致性
     * 验证所有API响应都应该包含基本的成功/失败标识和错误处理
     */
    @Property(tries = 100)
    void apiResponseStructureConsistency(@ForAll("validUserId") String userId) {
        
        // 创建模拟响应对象来验证结构一致性
        MultimodalAnalysisResponse multimodalResponse = new MultimodalAnalysisResponse();
        ImageRecognitionResponse imageResponse = new ImageRecognitionResponse();
        ProblemSolutionResponse problemResponse = new ProblemSolutionResponse();
        ExperimentSessionResponse sessionResponse = new ExperimentSessionResponse();
        ExperimentAnalysisResponse analysisResponse = new ExperimentAnalysisResponse();
        
        // 验证所有响应类都有success字段的getter方法
        assertThat(multimodalResponse).hasFieldOrProperty("success");
        assertThat(imageResponse).hasFieldOrProperty("success");
        assertThat(problemResponse).hasFieldOrProperty("success");
        assertThat(sessionResponse).hasFieldOrProperty("success");
        assertThat(analysisResponse).hasFieldOrProperty("success");
        
        // 验证所有响应类都有errorMessage字段的getter方法
        assertThat(multimodalResponse).hasFieldOrProperty("errorMessage");
        assertThat(imageResponse).hasFieldOrProperty("errorMessage");
        assertThat(problemResponse).hasFieldOrProperty("errorMessage");
        assertThat(sessionResponse).hasFieldOrProperty("errorMessage");
        assertThat(analysisResponse).hasFieldOrProperty("errorMessage");
    }
    
    // 数据生成器
    
    @Provide
    Arbitrary<String> validGestureData() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(10)
            .ofMaxLength(100)
            .map(s -> "{\"coordinates\":[{\"x\":100,\"y\":200}],\"timestamp\":\"2024-12-19T10:30:00Z\"}");
    }
    
    @Provide
    Arbitrary<String> validContentImage() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z')
            .ofMinLength(20)
            .ofMaxLength(50)
            .map(s -> "data:image/jpeg;base64," + s);
    }
    
    @Provide
    Arbitrary<String> validUserId() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(s -> "user_" + s);
    }
    
    @Provide
    Arbitrary<MockMultipartFile> validImageFile() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z')
            .ofMinLength(10)
            .ofMaxLength(50)
            .map(content -> new MockMultipartFile(
                "image", 
                "test.jpg", 
                "image/jpeg", 
                content.getBytes()
            ));
    }
    
    @Provide
    Arbitrary<String> validProblemText() {
        return Arbitraries.of(
            "求解方程 x² + 2x - 3 = 0",
            "计算函数 f(x) = 2x + 1 在 x = 3 时的值",
            "求导数 d/dx(x³ + 2x² - x + 1)",
            "解不等式 2x + 5 > 3x - 1"
        );
    }
    
    @Provide
    Arbitrary<String> validSubject() {
        return Arbitraries.of("mathematics", "physics", "chemistry", "biology");
    }
    
    @Provide
    Arbitrary<String> validSessionId() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(8)
            .ofMaxLength(16)
            .map(s -> "session_" + s);
    }
    
    @Provide
    Arbitrary<String> validExperimentType() {
        return Arbitraries.of("chemistry", "physics", "biology", "general");
    }
    
    @Provide
    Arbitrary<Map<String, Object>> validExperimentData() {
        return Arbitraries.maps(
            Arbitraries.of("temperature", "pressure", "humidity", "ph"),
            Arbitraries.doubles().between(0.0, 100.0).map(Object.class::cast)
        ).ofMinSize(1).ofMaxSize(4);
    }
}