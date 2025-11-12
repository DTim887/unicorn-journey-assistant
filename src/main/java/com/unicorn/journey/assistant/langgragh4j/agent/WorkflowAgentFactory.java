package com.unicorn.journey.assistant.langgragh4j.agent;

import com.unicorn.journey.assistant.service.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流 AI Agent 工厂
 * 负责创建和管理工作流中使用的 AI Agent
 */
@Service
@Slf4j
public class WorkflowAgentFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

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

    // Agent 缓存，key: sessionId + agentType
    private static final Map<String, Object> agentCache = new ConcurrentHashMap<>();

    /**
     * 获取plan创建 Agent
     */
    public WorkflowPlanAgent getPlanAgent(String sessionId) {
        String cacheKey = sessionId + "_PLAN";
        Object agent = agentCache.get(cacheKey);
        if (agent == null) {
            agent = createPlanAgent();
            agentCache.put(cacheKey, agent);
        }
        return (WorkflowPlanAgent) agent;
    }

    /**
     * 获取订单创建 Agent
     */
    public WorkflowOrderAgent getOrderAgent(String sessionId) {
        String cacheKey = sessionId + "_ORDER";
        Object agent = agentCache.get(cacheKey);
        if (agent == null) {
            agent = createOrderAgent();
            agentCache.put(cacheKey, agent);
        }
        return (WorkflowOrderAgent) agent;
    }

    /**
     * 获取统筹 Agent
     */
    public OrchestratorAgent getOrchestratorAgent(String sessionId) {
        String cacheKey = sessionId + "_ORCHESTRATOR";
        Object agent = agentCache.get(cacheKey);
        if (agent == null) {
            agent = createOrchestratorAgent();
            agentCache.put(cacheKey, agent);
        }
        return (OrchestratorAgent) agent;
    }

    /**
     * 清理 session 的 Agent 缓存
     */
    public void clearAgentCache(String sessionId) {
        agentCache.remove(sessionId + "_PLAN");
        agentCache.remove(sessionId + "_ORDER");
        agentCache.remove(sessionId + "_ORCHESTRATOR");
        log.info("清理 Agent 缓存: sessionId={}", sessionId);
    }

    /**
     * 创建plan Agent
     */
    private WorkflowPlanAgent createPlanAgent() {
        return AiServices.builder(WorkflowPlanAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .streamingChatModel(streamingChatModel)
                //.tools(List.of(userService, attractionService, planService))
                .build();
    }

    /**
     * 创建订单 Agent
     */
    private WorkflowOrderAgent createOrderAgent() {
        return AiServices.builder(WorkflowOrderAgent.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(50))
                .tools(List.of(userService,planService, productService, orderService))
                .build();
    }

    /**
     * 创建统筹 Agent
     */
    private OrchestratorAgent createOrchestratorAgent() {
        return AiServices.builder(OrchestratorAgent.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(50))
                .tools(List.of(userService,planService, productService, orderService))
                .build();
    }
}

