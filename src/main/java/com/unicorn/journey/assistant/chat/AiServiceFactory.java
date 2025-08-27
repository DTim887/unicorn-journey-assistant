package com.unicorn.journey.assistant.chat;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.AttractionService;
import com.unicorn.journey.assistant.service.BaseService;
import com.unicorn.journey.assistant.service.PlanService;
import com.unicorn.journey.assistant.service.UserService;
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

    public AiService getAiService(User user) {
        AiService aiService = this.get(user.getId());
        if(aiService == null) {
            aiService = createAiService(user.getId());
        }
        return aiService;
    }

    public AiService createAiService(int id) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        AiService aiService = AiServices.builder(AiService.class)
//               .chatModel(chatModel())
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> chatMemory)
//               .toolProvider(mcpToolProvider)  //mcp tool
                //register the tools
                .tools(List.of(userService, attractionService, planService))
                .build();
        this.put(id, aiService);
        return aiService;
    }
}
