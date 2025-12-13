package com.unicorn.journey.assistant.hotel.factory;

import com.unicorn.journey.assistant.hotel.agent.*;
import com.unicorn.journey.assistant.hotel.service.QueueTimeService;
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
    private ChatModel deepseekChatModel;

    @Resource
    private ChatModel deepseekChatModelWithHighTemperature;


    @Resource
    private QueueTimeService queueTimeService;


    /**
     * 创建路由Agent
     */
    public HotelRouterAgent createRouterAgent() {
        return AiServices.builder(HotelRouterAgent.class)
                .chatModel(deepseekChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
    
    /**
     * 创建点餐Agent（配置计算工具）
     */
    public MOAgent createMOAgent() {
        return AiServices.builder(MOAgent.class)
                .chatModel(deepseekChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
    
    /**
     * 创建叫醒服务Agent
     */
    public WakeUpAgent createWakeUpAgent() {
        return AiServices.builder(WakeUpAgent.class)
                .chatModel(deepseekChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    public QueueTimeAgent createQueueTimeAgent() {
        return AiServices.builder(QueueTimeAgent.class)
                .chatModel(deepseekChatModel)
                .tools(queueTimeService)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
    
    /**
     * 创建叫醒文案生成Agent
     */
    public WakeUpCopywritingAgent createWakeUpCopywritingAgent() {
        return AiServices.builder(WakeUpCopywritingAgent.class)
                .chatModel(deepseekChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
    
    /**
     * 创建总结Agent
     */
    public com.unicorn.journey.assistant.hotel.agent.SummaryAgent createSummaryAgent() {
        return AiServices.builder(com.unicorn.journey.assistant.hotel.agent.SummaryAgent.class)
                .chatModel(deepseekChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    public WakeUpCopywritingAgent createWakeUpCopywritingAgentWithHighTemperature() {
        return AiServices.builder(WakeUpCopywritingAgent.class)
                .chatModel(deepseekChatModelWithHighTemperature)
                .build();
    }
}
