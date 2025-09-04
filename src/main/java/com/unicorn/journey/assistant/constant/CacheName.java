package com.unicorn.journey.assistant.constant;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.entity.*;
import lombok.Getter;

@Getter
public enum CacheName{
    AISERVICE("AISERVICE", AiService.class),
    USERS("USERS", User.class),
    ORDERS("ORDERS", Order.class),
    ATTRACTION("ATTRACTION", Attraction.class),
    PLAN("PLAN", Plan.class),
    PRODUCT("PRODUCT", Product.class),
    ASSISTANT("PRODUCT", Assistant.class)
    ;
    private final String cacheName;
    private final Class<?> clazz;


    CacheName(String cacheName, Class<?> clazz) {
        this.cacheName = cacheName;
        this.clazz = clazz;
    }
}
