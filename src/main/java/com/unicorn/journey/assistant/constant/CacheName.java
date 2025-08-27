package com.unicorn.journey.assistant.constant;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.entity.Attraction;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.User;
import lombok.Getter;

@Getter
public enum CacheName{
    AISERVICE("AISERVICE", AiService.class),
    USERS("USERS", User.class),
    ORDERS("ORDERS", Order.class),
    ATTRACTION("ATTRACTION", Attraction.class),
    PLAN("PLAN", Plan.class),
    PRODUCT("PRODUCT", Product.class)
    ;
    private final String cacheName;
    private final Class<?> clazz;


    CacheName(String cacheName, Class<?> clazz) {
        this.cacheName = cacheName;
        this.clazz = clazz;
    }
}
