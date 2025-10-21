package com.unicorn.journey.assistant.chat;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.service.*;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.AISERVICE)
public class AiServiceFactory extends BaseService<AiService> {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private StreamingChatModel deepseekStreamingChatModel;

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
    private McpToolProvider mcpToolProvider;

    @Resource
    private RedNoteService redNoteService;

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
            case WENNIE, DUFFY, WOODY -> {
                yield List.of(productService,orderService);
            }
            case JUDY -> {
                yield List.of(attractionService, planService);
            }
        };
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(50);
        AiService aiService = AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(tools)
                .build();
        this.put(id, aiService);
        return aiService;
    }

    private AiService createDeepseekAiService(String id, Assistants assistant) {
        List<Object> tools = switch (assistant) {
            case WENNIE -> List.of(userService, productService,orderService);
            case DUFFY -> List.of(userService, productService, orderService);
            case JUDY -> List.of(userService, attractionService, planService, productService, orderService);
            case WOODY -> List.of(redNoteService);
        };
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(50);
        AiService aiService = AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(deepseekStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(tools)
                .build();
        this.put(id, aiService);
        return aiService;
    }

    public AiService getXiaoHongShuAiService() {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        return AiServices.builder(AiService.class)
//                .chatModel(chatModel)
                .streamingChatModel(deepseekStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(List.of(redNoteService))
                .toolProvider(mcpToolProvider)
                .build();
    }

}
