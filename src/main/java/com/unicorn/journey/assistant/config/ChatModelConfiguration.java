package com.unicorn.journey.assistant.config;


import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatModelConfiguration {

    @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name}")
    private String streamModelName;
    @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}")
    private String streamModelKey;

    @Value("${deepseek.base-url}")
    private String deepseekBaseUrl;
    @Value("${deepseek.api-key}")
    private String deepseekApiKey;
    @Value("${deepseek.model-name}")
    private String deepseekModelName;

    @Resource
    private CustomChatModelListener customChatModelListener;


    @Bean
    public OpenAiLanguageModel openAiLanguageModel() {
        return OpenAiLanguageModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName)
                .temperature(0.1)
                .build();
    }


    @Bean
    public StreamingChatModel deepseekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName) //deepseek 3.1
                .temperature(0.1)
                .listeners(List.of(customChatModelListener))
                .build();
    }

    @Bean
    public ChatModel deepseekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName) //deepseek 3.1
                .temperature(0.1)
                .listeners(List.of(customChatModelListener))
                .build();
    }


    @Bean
    public StreamingChatModel   streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .modelName(streamModelName)
                .apiKey(streamModelKey)
                .temperature(0.3f)
                .listeners(List.of(customChatModelListener))
                .build();
    }

    @Bean
    public ChatModel chatModel() {
        return QwenChatModel.builder()
                .modelName(streamModelName)
                .apiKey(streamModelKey)
                .temperature(0.3f)
                .listeners(List.of(customChatModelListener))
                .build();
    }


}
