package com.unicorn.journey.assistant.config;


import com.unicorn.journey.assistant.service.UserService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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

    @Resource
    private UserService userService;

    @Resource
    private CustomChatModelListener customChatModelListener;


    @Bean
    public StreamingChatModel deepseekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey("sk-d1cdeff51b17440089f5860dc6c1d04e")
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-reasoner") //deepseek 3.1
                .temperature(0.3)
                .listeners(List.of(customChatModelListener))
                .build();
    }


    @Bean
    public StreamingChatModel streamingChatModel() {
//        return GoogleAiGeminiStreamingChatModel.builder()
//                .modelName("gemini-2.5-flash-lite")
//                .apiKey("AIzaSyDxySH9-KCJb1F9Ec2SSMeqyb0jdEKd3yA")
//                .temperature(0.0)
//                .listeners(List.of(customChatModelListener))
//                .build();
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
