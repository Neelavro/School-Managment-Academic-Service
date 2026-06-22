package com.example.academic_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableAsync
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("studentResult",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(10, TimeUnit.DAYS)
                        .build());
        // Platform metrics snapshot — recomputed at most once per minute
        // so repeated polls don't hammer the DB.
        manager.registerCustomCache("platformMetrics",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());
        return manager;
    }
}
