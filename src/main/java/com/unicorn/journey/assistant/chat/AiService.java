package com.unicorn.journey.assistant.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiService  {

    //和 judy 聊天
    @SystemMessage(fromResource = "system_prompt.txt")
    Flux<String> judyChat(@MemoryId String memoryId , @UserMessage String userMessage);


    //和 duffy 聊天
    @SystemMessage(fromResource = "duffy_prompt.txt")
    Flux<String> duffyChat(@MemoryId String memoryId , @UserMessage String userMessage);
}
