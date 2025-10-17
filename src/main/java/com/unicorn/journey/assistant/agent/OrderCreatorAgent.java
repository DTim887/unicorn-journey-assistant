package com.unicorn.journey.assistant.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.UserMessage;

//行程规划代理
public interface OrderCreatorAgent {

    AiMessage plan(@UserMessage String userMessage);
}
