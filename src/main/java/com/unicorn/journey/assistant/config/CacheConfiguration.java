package com.unicorn.journey.assistant.config;

import com.unicorn.journey.assistant.constant.CacheName;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCacheNames(Arrays.stream(CacheName.values()).map(CacheName::getCacheName).toList());
        return caffeineCacheManager;
    }
}
