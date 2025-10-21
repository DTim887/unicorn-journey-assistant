package com.unicorn.journey.assistant.chat;

import com.unicorn.journey.assistant.entity.User;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

public interface AiService  {

    //和 judy 聊天
    @SystemMessage(fromResource = "judy_prompt.txt")
    Flux<String> judyChat(@MemoryId String memoryId , @UserMessage String userMessage, @V("user") User user);

    //和 duffy 聊天
    @SystemMessage(fromResource = "duffy_prompt.txt")
    Flux<String> duffyChat(@MemoryId String memoryId , @UserMessage String userMessage, @V("user") User user);

    //和 wennie 聊天
    @SystemMessage(fromResource = "wennie_prompt.txt")
    Flux<String> wennieChat(@MemoryId String memoryId , @UserMessage String userMessage, @V("user") User user);

    //和 胡迪 聊天
    @SystemMessage(fromResource = "duffy_prompt.txt")
    Flux<String> woodyChat(@MemoryId String memoryId , @UserMessage String userMessage, @V("user") User user);

    @SystemMessage(fromResource = "prompt/xiaohongshu-prompt.txt")
    Flux<String> xiaoHongShu (@MemoryId String memoryId , @UserMessage String userMessage);

}
