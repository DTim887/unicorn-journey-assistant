package com.unicorn.journey.assistant.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiService {

    //This is Agent System Prompt
    @SystemMessage(fromResource = "system_prompt.txt")
    Flux<String> streamChat(@MemoryId int memoryId, @UserMessage String userMessage);
}
