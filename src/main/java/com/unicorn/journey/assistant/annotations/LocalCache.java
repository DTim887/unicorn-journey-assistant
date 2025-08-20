package com.unicorn.journey.assistant.annotations;

import com.unicorn.journey.assistant.constant.CacheName;

import java.lang.annotation.*;

@Target(ElementType.TYPE)  // 标注在类上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LocalCache {
    CacheName value(); // 缓存名
}
