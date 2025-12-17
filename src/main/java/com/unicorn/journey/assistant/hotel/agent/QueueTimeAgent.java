package com.unicorn.journey.assistant.hotel.agent;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface QueueTimeAgent {


    @SystemMessage(fromResource = "/prompt/queue-time-prompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "/prompt/in-park-prompt_v2.txt")
    String inParkChat(@MemoryId String memoryId, @UserMessage String userMessage);

}
