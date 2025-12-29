package com.ithra.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheClearRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheClearRunner.class);

    private final CacheManager cacheManager;

    public CacheClearRunner(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void run(String... args) {
        try {
            logger.info("🔄 Clearing all Redis caches on startup...");

            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.info("✅ Cleared cache: {}", cacheName);
                }
            });

            logger.info("✅ All caches cleared successfully");
        } catch (Exception e) {
            logger.error("❌ Error clearing caches: {}", e.getMessage(), e);
        }
    }
}