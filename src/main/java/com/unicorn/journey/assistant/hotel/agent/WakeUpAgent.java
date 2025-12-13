package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 陪伴服务Agent - 提供陪伴服务和夜间讲故事服务
 */
public interface WakeUpAgent {
    
    @SystemMessage(fromResource = "/prompt/wake-up-prompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
