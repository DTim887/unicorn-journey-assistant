package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 房间叫醒服务Agent
 * 提供叫醒服务和睡前故事，输出JSON结构化数据
 */
public interface RoomWakeUpAgent {
    
    @SystemMessage(fromResource = "/prompt/wake-up-prompt_v2.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
