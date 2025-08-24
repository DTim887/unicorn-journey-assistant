package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public abstract class BaseService<T> {

    private CacheName cacheName;

    private static class SessionManagerHolder {
        private static final CaffeineCacheManager cacheManager = SpringBeanUtils.getBean(CaffeineCacheManager.class);
    }

    @PostConstruct
    private void init() {
        Class<?> clazz = this.getClass();
        if (clazz.isAnnotationPresent(LocalCache.class)) {
            LocalCache annotation = clazz.getAnnotation(LocalCache.class);
            this.cacheName = annotation.value();
        } else {
            throw new IllegalStateException("Service must be annotated with @LocalCache");
        }
        logger.info("缓存名: {}", cacheName.getCacheName());

    }


    private static final Logger logger = LoggerFactory.getLogger(BaseService.class);

    /**
     * 通用方法：根据缓存Key获取缓存对象
     */
    public T get(Object cacheKey) {
        return getInternal(cacheKey);
    }

    /**
     * 通用方法：获取所有缓存对象
     */
    public List<T> getAll(Class<T> tClass) {

        ConcurrentMap<Object, Object> map = getCache().getNativeCache().asMap();
        Collection<Object> values = map.values();
        // 过滤并转换为指定类型
        List<T> result = new ArrayList<>();
        for (Object value : values) {
            if (tClass.isInstance(value)) {
                result.add(tClass.cast(value));
            }
        }
        return result;
    }

    /**
     * 存入缓存
     */
    public void put(Object cacheKey, Object value) {
        getCache().put(cacheKey, value);
    }

    /**
     * 删除缓存
     */
    public void evict(Object cacheKey) {
        getCache().evict(cacheKey);
    }

    @SuppressWarnings("unchecked")
    private T getInternal(Object cacheKey) {
        Objects.requireNonNull(cacheKey, "缓存键不能为空");

        Cache.ValueWrapper wrapper = getCache().get(cacheKey);
        if (wrapper == null) {
            logger.warn("缓存未命中: {}", cacheKey);
            return null; // 缓存未命中
        }

        Object obj = wrapper.get();
        Class<T> type = (Class<T>) cacheName.getClazz();

        if (type.isInstance(obj)) {
            return type.cast(obj);
        } else {
            logger.warn("类型不匹配: 期望 {}, 实际 {}", type.getName(), obj != null ? obj.getClass().getName() : "null");
            return null;
        }
    }

    private CaffeineCache getCache() {
        CaffeineCache cache = (CaffeineCache) SessionManagerHolder.cacheManager.getCache(cacheName.getCacheName());
        if (cache == null) {
            logger.error("缓存不存在: {}", cacheName.getCacheName());
            throw new IllegalStateException("Cache must not be null");
        }
        return cache;
    }

}
