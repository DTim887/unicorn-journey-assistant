package com.unicorn.journey.assistant.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiAgent {

    @SystemMessage(fromResource = "weather-prompt.txt")
    CreatePlanResponse weather(@MemoryId String memoryId , @UserMessage String userMessage);

    @SystemMessage(fromResource = "plan-prompt.txt")
    String plan(@MemoryId String memoryId , @UserMessage String userMessage);

    String test(@MemoryId String memoryId , @UserMessage String userMessage);
}
