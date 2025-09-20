package com.unicorn.journey.assistant.chat;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.client.McpClient;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.service.AttractionService;
import com.unicorn.journey.assistant.service.BaseService;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.PlanService;
import com.unicorn.journey.assistant.service.ProductService;
import com.unicorn.journey.assistant.service.UserService;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;

@Service
@LocalCache(value = CacheName.AISERVICE)
public class AiServiceFactory extends BaseService<AiService> {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private StreamingChatModel deepseekStreamingChatModel;

//    @Resource
//    private MessageWindowChatMemory messageWindowChatMemory;

    @Resource
    private ChatModel chatModel;

    @Resource
    private UserService userService;

    @Resource
    private AttractionService attractionService;

    @Resource
    private PlanService planService;

    @Resource
    private OrderService orderService;

    @Resource
    private ProductService productService;
    
    @Resource
    private McpClient mcpClient;

    //@Resource
    //private ContentRetriever contentRetriever;

    public AiService getDeepseekAiService(String id, Assistants assistant) {
        AiService aiService = this.get(id);
        if (aiService == null) {
            aiService = createDeepseekAiService(id, assistant);
        }
        return aiService;
    }

    public AiService getAiService(String id, Assistants assistant) {
        AiService aiService = this.get(id);
        if (aiService == null) {
            aiService = createAiService(id, assistant);
        }
        return aiService;
    }

    //重启会话
    public void newConversation(String id) {
        this.evict(id);
    }


    private AiService createAiService(String id, Assistants assistant) {
        List<Object> tools = switch (assistant) {
            case WENNIE -> List.of(userService, productService, orderService, mcpClient);
            case DUFFY -> List.of(userService, productService, orderService);
            case JUDY -> List.of(userService, attractionService, planService, productService, orderService);
            case WOODY -> List.of(userService, productService, orderService);
        };
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        AiService aiService = AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//                .contentRetriever(contentRetriever)
                //register the tools
                .tools(tools)
                .build();
        this.put(id, aiService);
        return aiService;
    }

    private AiService createDeepseekAiService(String id, Assistants assistant) {
        List<Object> tools = switch (assistant) {
            case WENNIE -> List.of(userService, productService, orderService, mcpClient);
            case DUFFY -> List.of(userService, productService, orderService);
            case JUDY -> List.of(userService, attractionService, planService, productService, orderService);
            case WOODY -> List.of(userService, productService, orderService);
        };
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        AiService aiService = AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(deepseekStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//                .contentRetriever(contentRetriever)
                //register the tools
                .tools(tools)
                .build();
        this.put(id, aiService);
        return aiService;
    }

}
