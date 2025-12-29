package com.ithra.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;

public class CustomCacheErrorHandler implements CacheErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (exception.getCause() instanceof SerializationException) {
            logger.warn("⚠️ Deserialization error for cache '{}' key '{}'. Evicting bad entry.",
                    cache.getName(), key);
            try {
                cache.evict(key); // Remove the corrupted cache entry
                logger.info("✅ Evicted corrupted cache entry: {}", key);
            } catch (Exception e) {
                logger.error("❌ Failed to evict cache entry: {}", e.getMessage());
            }
        } else {
            logger.error("❌ Cache GET error in '{}' for key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logger.error("❌ Cache PUT error in '{}' for key '{}': {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logger.error("❌ Cache EVICT error in '{}' for key '{}': {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logger.error("❌ Cache CLEAR error in '{}': {}",
                cache.getName(), exception.getMessage());
    }
}