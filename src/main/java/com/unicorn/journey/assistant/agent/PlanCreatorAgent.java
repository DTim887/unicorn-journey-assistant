package com.unicorn.journey.assistant.agent;

import com.unicorn.journey.assistant.chat.CreatePlanResponse;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

//行程创建代理
public interface PlanCreatorAgent {

    @SystemMessage(fromResource = "prompt/weather-prompt.txt")
    CreatePlanResponse plan(@MemoryId String memoryId, @UserMessage String userMessage);

}
