package com.education.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis缓存服务单元测试
 * 测试Redis缓存操作的正确性和异常处理
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheServiceTest {
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // 忽略清理异常
        }
    }
    
    @Test
    void testSetAndGet() {
        String key = "test:key1";
        String value = "test_value";
        
        // 测试设置缓存
        cacheService.set(key, value);
        
        // 测试获取缓存
        Object retrievedValue = cacheService.get(key);
        assertEquals(value, retrievedValue);
    }
    
    @Test
    void testSetWithTimeout() {
        String key = "test:key2";
        String value = "test_value_with_timeout";
        Duration timeout = Duration.ofSeconds(10);
        
        // 测试设置带过期时间的缓存
        cacheService.set(key, value, timeout);
        
        // 验证缓存存在
        Object retrievedValue = cacheService.get(key);
        assertEquals(value, retrievedValue);
        
        // 验证过期时间设置
        long expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= 10);
    }
    
    @Test
    void testSetWithTimeoutSeconds() {
        String key = "test:key3";
        String value = "test_value_with_timeout_seconds";
        long timeoutSeconds = 5L;
        
        // 测试设置带过期时间的缓存（秒）
        cacheService.set(key, value, timeoutSeconds);
        
        // 验证缓存存在
        Object retrievedValue = cacheService.get(key);
        assertEquals(value, retrievedValue);
        
        // 验证过期时间设置
        long expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= timeoutSeconds);
    }
    
    @Test
    void testGetWithType() {
        String key = "test:key4";
        String value = "typed_value";
        
        // 设置缓存
        cacheService.set(key, value);
        
        // 测试类型转换获取
        String typedValue = cacheService.get(key, String.class);
        assertEquals(value, typedValue);
        
        // 测试错误类型转换
        Integer wrongType = cacheService.get(key, Integer.class);
        assertNull(wrongType);
    }
    
    @Test
    void testDelete() {
        String key = "test:key5";
        String value = "value_to_delete";
        
        // 设置缓存
        cacheService.set(key, value);
        assertTrue(cacheService.exists(key));
        
        // 测试删除缓存
        boolean deleted = cacheService.delete(key);
        assertTrue(deleted);
        assertFalse(cacheService.exists(key));
        
        // 测试删除不存在的键
        boolean notDeleted = cacheService.delete("non_existent_key");
        assertFalse(notDeleted);
    }
    
    @Test
    void testBatchDelete() {
        String key1 = "test:batch1";
        String key2 = "test:batch2";
        String key3 = "test:batch3";
        
        // 设置多个缓存
        cacheService.set(key1, "value1");
        cacheService.set(key2, "value2");
        cacheService.set(key3, "value3");
        
        // 验证缓存存在
        assertTrue(cacheService.exists(key1));
        assertTrue(cacheService.exists(key2));
        assertTrue(cacheService.exists(key3));
        
        // 测试批量删除
        long deletedCount = cacheService.delete(key1, key2, key3);
        assertEquals(3, deletedCount);
        
        // 验证缓存已删除
        assertFalse(cacheService.exists(key1));
        assertFalse(cacheService.exists(key2));
        assertFalse(cacheService.exists(key3));
    }
    
    @Test
    void testExists() {
        String key = "test:exists";
        String value = "exists_value";
        
        // 测试不存在的键
        assertFalse(cacheService.exists(key));
        
        // 设置缓存后测试存在性
        cacheService.set(key, value);
        assertTrue(cacheService.exists(key));
    }
    
    @Test
    void testExpire() {
        String key = "test:expire";
        String value = "expire_value";
        
        // 设置缓存
        cacheService.set(key, value);
        
        // 测试设置过期时间
        Duration timeout = Duration.ofSeconds(5);
        boolean expireSet = cacheService.expire(key, timeout);
        assertTrue(expireSet);
        
        // 验证过期时间
        long expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= 5);
    }
    
    @Test
    void testExpireSeconds() {
        String key = "test:expire_seconds";
        String value = "expire_seconds_value";
        
        // 设置缓存
        cacheService.set(key, value);
        
        // 测试设置过期时间（秒）
        long timeoutSeconds = 3L;
        boolean expireSet = cacheService.expire(key, timeoutSeconds);
        assertTrue(expireSet);
        
        // 验证过期时间
        long expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= timeoutSeconds);
    }
    
    @Test
    void testGetExpire() {
        String key = "test:get_expire";
        String value = "get_expire_value";
        
        // 设置永久缓存
        cacheService.set(key, value);
        long expireTime = cacheService.getExpire(key);
        assertEquals(-1, expireTime); // 永久缓存返回-1
        
        // 设置带过期时间的缓存
        cacheService.set(key, value, 10L);
        expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= 10);
        
        // 测试不存在的键
        long nonExistentExpire = cacheService.getExpire("non_existent_key");
        assertEquals(-1, nonExistentExpire);
    }
    
    @Test
    void testIncrement() {
        String key = "test:increment";
        
        // 测试递增不存在的键
        long result1 = cacheService.increment(key);
        assertEquals(1, result1);
        
        // 测试递增已存在的键
        long result2 = cacheService.increment(key);
        assertEquals(2, result2);
        
        long result3 = cacheService.increment(key);
        assertEquals(3, result3);
    }
    
    @Test
    void testIncrementWithTimeout() {
        String key = "test:increment_timeout";
        Duration timeout = Duration.ofSeconds(5);
        
        // 测试递增并设置过期时间
        long result1 = cacheService.increment(key, timeout);
        assertEquals(1, result1);
        
        // 验证过期时间设置
        long expireTime = cacheService.getExpire(key);
        assertTrue(expireTime > 0 && expireTime <= 5);
        
        // 再次递增，过期时间不应该重新设置
        long result2 = cacheService.increment(key, timeout);
        assertEquals(2, result2);
    }
    
    @Test
    void testKeys() {
        String pattern = "test:pattern:*";
        String key1 = "test:pattern:key1";
        String key2 = "test:pattern:key2";
        String key3 = "test:other:key3";
        
        // 设置测试数据
        cacheService.set(key1, "value1");
        cacheService.set(key2, "value2");
        cacheService.set(key3, "value3");
        
        // 测试模式匹配
        Set<String> matchedKeys = cacheService.keys(pattern);
        assertTrue(matchedKeys.contains(key1));
        assertTrue(matchedKeys.contains(key2));
        assertFalse(matchedKeys.contains(key3));
    }
    
    @Test
    void testCacheKeyGenerators() {
        // 测试缓存键生成器
        String userId = "user123";
        String userCacheKey = CacheService.userCacheKey(userId);
        assertEquals("user:user123", userCacheKey);
        
        String analysisId = "analysis456";
        String gestureCacheKey = CacheService.gestureCacheKey(analysisId);
        assertEquals("gesture:analysis456", gestureCacheKey);
        
        String sessionId = "session789";
        String experimentCacheKey = CacheService.experimentCacheKey(sessionId);
        assertEquals("experiment:session789", experimentCacheKey);
        
        String apiName = "multimodal-api";
        String requestHash = "hash123";
        String apiCacheKey = CacheService.apiCacheKey(apiName, requestHash);
        assertEquals("api:multimodal-api:hash123", apiCacheKey);
    }
    
    @Test
    void testComplexObjectCaching() {
        String key = "test:complex_object";
        
        // 创建复杂对象进行缓存测试
        TestCacheObject testObject = new TestCacheObject("test_name", 123, true);
        
        // 设置复杂对象缓存
        cacheService.set(key, testObject);
        
        // 获取并验证复杂对象
        TestCacheObject retrievedObject = cacheService.get(key, TestCacheObject.class);
        assertNotNull(retrievedObject);
        assertEquals("test_name", retrievedObject.getName());
        assertEquals(123, retrievedObject.getValue());
        assertTrue(retrievedObject.isActive());
    }
    
    @Test
    void testCacheOperationsWithNullValues() {
        String key = "test:null_value";
        
        // 测试获取不存在的键
        Object nullValue = cacheService.get(key);
        assertNull(nullValue);
        
        // 测试类型转换获取不存在的键
        String nullTypedValue = cacheService.get(key, String.class);
        assertNull(nullTypedValue);
        
        // 测试删除不存在的键
        boolean deleteResult = cacheService.delete(key);
        assertFalse(deleteResult);
        
        // 测试设置过期时间给不存在的键
        boolean expireResult = cacheService.expire(key, Duration.ofSeconds(10));
        assertFalse(expireResult);
    }
    
    /**
     * 测试缓存对象类
     */
    public static class TestCacheObject {
        private String name;
        private int value;
        private boolean active;
        
        public TestCacheObject() {}
        
        public TestCacheObject(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}