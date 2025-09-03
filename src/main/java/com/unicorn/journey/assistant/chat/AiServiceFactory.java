package com.unicorn.journey.assistant.chat;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.client.McpClient;
import com.unicorn.journey.assistant.service.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.AISERVICE)
public class AiServiceFactory extends BaseService<AiService> {

    @Resource
    private StreamingChatModel  streamingChatModel;

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


    public AiService getAiService(String id) {

        AiService aiService = this.get(id);
        if(aiService == null) {
            aiService = createAiService(id);
        }
        return aiService;
    }

    public AiService createAiService(String id) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        AiService aiService = AiServices.builder(AiService.class)
//               .chatModel(chatModel())
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//               .toolProvider(mcpToolProvider)  //mcp tool
                //register the tools
                .tools(List.of(userService, attractionService, planService, orderService, productService, mcpClient))
                .build();
        this.put(id, aiService);
        return aiService;
    }
}
