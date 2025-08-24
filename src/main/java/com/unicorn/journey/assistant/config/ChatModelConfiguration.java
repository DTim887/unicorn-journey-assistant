package com.unicorn.journey.assistant.config;


import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.service.FacilityService;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.UserService;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatModelConfiguration {

    @Value("${langchain4j.community.dashscope.chat-model.model-name}")
    private String streamModelName;
    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String streamModelKey;

    @Resource
    private UserService userService;

    @Resource
    private FacilityService facilityService;

    @Resource
    private CustomChatModelListener customChatModelListener;



    @Bean
    public StreamingChatModel streamingChatModel() {
//        return GoogleAiGeminiStreamingChatModel.builder()
//                .modelName("gemini-2.5-flash-lite")
//                .apiKey("AIzaSyDxySH9-KCJb1F9Ec2SSMeqyb0jdEKd3yA")
//                .temperature(0.3)
//                .listeners(List.of(customChatModelListener))
//                .build();
        return QwenStreamingChatModel.builder()
                .modelName(streamModelName)
                .apiKey(streamModelKey)
                .temperature(0.3f)
                .listeners(List.of(customChatModelListener))
                .build();
    }

//    @Bean
//    public ChatModel chatModel() {
//        return QwenChatModel.builder()
//                .temperature(0.3f)
////                .listeners(List.of(myChatModelListener))
//                .build();
//    }

    @Bean
    public AiService aiServices(OrderService orderService){
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        return AiServices.builder(AiService.class)
//                .chatModel(chatModel())
                .streamingChatModel(streamingChatModel())
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//                .toolProvider(mcpToolProvider)  //mcp tool
                //register the tools
                .tools(List.of(userService, facilityService))
                .build();
    }


}
