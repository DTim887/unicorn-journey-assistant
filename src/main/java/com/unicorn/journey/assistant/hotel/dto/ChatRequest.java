package com.unicorn.journey.assistant.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    private String userId;
    
    private String message;
    
    private String sessionId;
    
    /**
     * 是否启用语音输出，默认false
     */
    private Boolean enableVoiceOutput = false;
}
