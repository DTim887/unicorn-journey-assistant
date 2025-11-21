package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Plan Agent - hotel行程规划代理
 */
public interface HotelPlanAgent {
    
    @SystemMessage(fromResource = "prompt/hotel_plan_prompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage, @V("userTags") String userTags);
}
