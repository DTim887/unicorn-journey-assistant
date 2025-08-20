package com.unicorn.journey.assistant.constant;

import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.User;
import lombok.Getter;

@Getter
public enum CacheName{

    USERS("USERS", User.class),
    ORDERS("ORDERS", Order.class),
    ;
    private final String cacheName;
    private final Class<?> clazz;


    CacheName(String cacheName, Class<?> clazz) {
        this.cacheName = cacheName;
        this.clazz = clazz;
    }
}
