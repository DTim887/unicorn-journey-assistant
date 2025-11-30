package com.unicorn.journey.assistant.hotel.factory;

import com.unicorn.journey.assistant.hotel.agent.HotelRouterAgent;
import com.unicorn.journey.assistant.hotel.agent.MOAgent;
import com.unicorn.journey.assistant.hotel.agent.WakeUpAgent;
import com.unicorn.journey.assistant.hotel.agent.WakeUpCopywritingAgent;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 酒店Agent工厂类
 * 使用ChatModelConfiguration中配置的ChatModel
 */
@Component
public class HotelAgentFactory {
    
    @Resource
    private ChatModel chatModel;
    
    /**
     * 创建路由Agent
     */
    public HotelRouterAgent createRouterAgent() {
        return AiServices.builder(HotelRouterAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
    
    /**
     * 创建点餐Agent
     */
    public MOAgent createMOAgent() {
        return AiServices.builder(MOAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
    
    /**
     * 创建叫醒服务Agent
     */
    public WakeUpAgent createWakeUpAgent() {
        return AiServices.builder(WakeUpAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
    
    /**
     * 创建叫醒文案生成Agent
     */
    public WakeUpCopywritingAgent createWakeUpCopywritingAgent() {
        return AiServices.builder(WakeUpCopywritingAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(5))
                .build();
    }
    
    /**
     * 创建总结Agent
     */
    public com.unicorn.journey.assistant.hotel.agent.SummaryAgent createSummaryAgent() {
        return AiServices.builder(com.unicorn.journey.assistant.hotel.agent.SummaryAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(2))
                .build();
    }
}
