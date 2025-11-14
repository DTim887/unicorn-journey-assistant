package com.unicorn.journey.assistant.langgragh4j.hotel.model;

import lombok.Data;

import java.util.Map;

/**
 * 酒店助手请求
 */
@Data
public class HotelAssistantRequest {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户消息
     */
    private String userMessage;
}
