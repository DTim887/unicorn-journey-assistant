package com.unicorn.journey.assistant.chat;

import com.unicorn.journey.assistant.agent.OrderCreatorAgent;
import com.unicorn.journey.assistant.agent.PlanCreatorAgent;
import com.unicorn.journey.assistant.constant.Agents;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.langgraph.demo.TestTool;
import com.unicorn.journey.assistant.langgraph.demo.WeatherTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAgentFactory {

    //最多工具连续调用次数
    private final int MAX_TOOLS_INVOCATIONS = 5;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private StreamingChatModel deepseekStreamingChatModel;

    @Resource
    private ChatModel chatModel;

    @Resource
    private ChatModel deepseekChatModel;

    private static final Map<String,Object> agentCache = new ConcurrentHashMap<>();


    public Object getAiAgent(Agents type, String workflowId) {
        Object object = agentCache.get(type + workflowId);
        if (object == null) {
            object = createAiAgent(type, workflowId);
        }
        return object;
    }

    private Object createAiAgent(Agents type, String workflowId) {
        Object object = switch (type) {
            case PLAN_CREATOR -> AiServices.builder(PlanCreatorAgent.class)
                       .chatModel(chatModel)
                       .streamingChatModel(streamingChatModel)
                       .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                       .tools(new WeatherTool())
                       .maxSequentialToolsInvocations(MAX_TOOLS_INVOCATIONS) //最多调用5次工具
                       .build();
            case ORDER_CREATOR -> AiServices.builder(OrderCreatorAgent.class).build();
        };
        agentCache.put(type + workflowId,object);
        return object;
    }

    //从缓存中删除Agent
    public void removeAiAgent(Agents type, String workflowId) {
        agentCache.remove(type + workflowId);
    }

    public AiAgent createAiService(Assistants assistant) {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(50);
        return switch (assistant) {
            case JUDY -> {
               yield  AiServices.builder(AiAgent.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(new TestTool())
                        .maxSequentialToolsInvocations(MAX_TOOLS_INVOCATIONS)
                        .build();
            }
            case DUFFY, WENNIE, WOODY -> {
                yield  AiServices.builder(AiAgent.class)
                        .chatModel(deepseekChatModel)
                        .streamingChatModel(deepseekStreamingChatModel)
                        .chatMemory(chatMemory)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools()
                        .maxSequentialToolsInvocations(MAX_TOOLS_INVOCATIONS)
                        .build();
            }
        };
    }
}
