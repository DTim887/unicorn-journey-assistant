package com.unicorn.journey.assistant.config;


import com.unicorn.journey.assistant.chat.AiService;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfiguration {

    @Value("${langchain4j.community.dashscope.chat-model.model-name}")
    private String streamModelName;
    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String streamModelKey;


    @Bean
    public StreamingChatModel streamingChatModel() {
        return QwenStreamingChatModel.builder()
                .modelName(streamModelName)
                .apiKey(streamModelKey)
                .temperature(0.3f)
//                .listeners(List.of(myChatModelListener))
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
    public AiService aiServices(){
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        return AiServices.builder(AiService.class)
//                .chatModel(chatModel())
                .streamingChatModel(streamingChatModel())
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//                .toolProvider(mcpToolProvider)  //mcp tool
//                .tools(List.of(testToolService))  //register the tool
                .build();
    }


}
