package com.unicorn.journey.assistant.chat;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiService  {

    //和 judy 聊天
    @SystemMessage(fromResource = "judy_property.txt")
    Flux<String> judyChat(@MemoryId String memoryId , @UserMessage String userMessage);


    //和 duffy 聊天
    @SystemMessage(fromResource = "duffy_prompt.txt")
    Flux<String> duffyChat(@MemoryId String memoryId , @UserMessage String userMessage);
    
    //和 stella 聊天
    @SystemMessage(fromResource = "stella_refund.txt")
    Flux<String> stellaChat(@MemoryId String memoryId , @UserMessage String userMessage);
}
