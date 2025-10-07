package com.unicorn.journey.assistant.langgragh4j.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能路由工厂
 */
@Configuration
public class RouterServiceFactory {

    @Resource
    @Qualifier("deepSeekChatModel")
    private ChatModel deepSeekChatModel;

    @Resource
    private StreamingChatModel deepseekStreamingChatModel;

    @Bean
    public RouterService createRouterService() {
        return AiServices.builder(RouterService.class)
                .chatModel(deepSeekChatModel)
                .streamingChatModel(deepseekStreamingChatModel)
                .build();
    }
}