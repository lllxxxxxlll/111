package com.education.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 提供统一的缓存操作接口
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 设置缓存值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            logger.debug("Cache set: key={}", key);
        } catch (Exception e) {
            logger.error("Failed to set cache: key={}", key, e);
        }
    }
    
    /**
     * 设置缓存值并指定过期时间
     */
    public void set(String key, Object value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
            logger.debug("Cache set with timeout: key={}, timeout={}", key, timeout);
        } catch (Exception e) {
            logger.error("Failed to set cache with timeout: key={}, timeout={}", key, timeout, e);
        }
    }
    
    /**
     * 设置缓存值并指定过期时间（秒）
     */
    public void set(String key, Object value, long timeoutSeconds) {
        set(key, value, Duration.ofSeconds(timeoutSeconds));
    }
    
    /**
     * 获取缓存值
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            logger.debug("Cache get: key={}, found={}", key, value != null);
            return value;
        } catch (Exception e) {
            logger.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 获取缓存值并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to get cache with type: key={}, type={}", key, type.getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            logger.debug("Cache delete: key={}, success={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to delete cache: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 批量删除缓存
     */
    public long delete(String... keys) {
        try {
            Long result = redisTemplate.delete(Set.of(keys));
            logger.debug("Cache batch delete: keys={}, deleted={}", keys.length, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("Failed to batch delete cache: keys={}", keys.length, e);
            return 0;
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to check cache existence: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 设置缓存过期时间
     */
    public boolean expire(String key, Duration timeout) {
        try {
            Boolean result = redisTemplate.expire(key, timeout);
            logger.debug("Cache expire set: key={}, timeout={}, success={}", key, timeout, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to set cache expiration: key={}, timeout={}", key, timeout, e);
            return false;
        }
    }
    
    /**
     * 设置缓存过期时间（秒）
     */
    public boolean expire(String key, long timeoutSeconds) {
        return expire(key, Duration.ofSeconds(timeoutSeconds));
    }
    
    /**
     * 获取缓存剩余过期时间
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            logger.error("Failed to get cache expiration: key={}", key, e);
            return -1;
        }
    }
    
    /**
     * 递增缓存值
     */
    public long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            logger.debug("Cache increment: key={}, result={}", key, result);
            return result != null ? result : 0;
        } catch (Exception e) {
            logger.error("Failed to increment cache: key={}", key, e);
            return 0;
        }
    }
    
    /**
     * 递增缓存值并设置过期时间
     */
    public long increment(String key, Duration timeout) {
        try {
            long result = increment(key);
            if (result == 1) { // 第一次设置时添加过期时间
                expire(key, timeout);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to increment cache with timeout: key={}, timeout={}", key, timeout, e);
            return 0;
        }
    }
    
    /**
     * 清空所有缓存（谨慎使用）
     */
    public void flushAll() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            logger.warn("All cache flushed");
        } catch (Exception e) {
            logger.error("Failed to flush all cache", e);
        }
    }
    
    /**
     * 获取缓存键的模式匹配
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            logger.error("Failed to get keys with pattern: pattern={}", pattern, e);
            return Set.of();
        }
    }
    
    // 缓存键前缀常量
    public static final String USER_CACHE_PREFIX = "user:";
    public static final String GESTURE_CACHE_PREFIX = "gesture:";
    public static final String EXPERIMENT_CACHE_PREFIX = "experiment:";
    public static final String API_CACHE_PREFIX = "api:";
    public static final String SESSION_CACHE_PREFIX = "session:";
    
    /**
     * 生成用户缓存键
     */
    public static String userCacheKey(String userId) {
        return USER_CACHE_PREFIX + userId;
    }
    
    /**
     * 生成手势分析缓存键
     */
    public static String gestureCacheKey(String analysisId) {
        return GESTURE_CACHE_PREFIX + analysisId;
    }
    
    /**
     * 生成实验会话缓存键
     */
    public static String experimentCacheKey(String sessionId) {
        return EXPERIMENT_CACHE_PREFIX + sessionId;
    }
    
    /**
     * 生成API调用缓存键
     */
    public static String apiCacheKey(String apiName, String requestHash) {
        return API_CACHE_PREFIX + apiName + ":" + requestHash;
    }
}