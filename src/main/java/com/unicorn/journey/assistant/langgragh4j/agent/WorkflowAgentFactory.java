package com.unicorn.journey.assistant.langgragh4j.agent;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // Agent 缓存，key: sessionId + agentType
    private static final Map<String, Object> agentCache = new ConcurrentHashMap<>();

    /**
     * 获取行程创建 Agent
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
     * 创建行程 Agent
     */
    private WorkflowPlanAgent createPlanAgent() {
        return AiServices.builder(WorkflowPlanAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(5))
                .build();
    }

    /**
     * 创建订单 Agent
     */
    private WorkflowOrderAgent createOrderAgent() {
        return AiServices.builder(WorkflowOrderAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(5))
                .build();
    }

    /**
     * 创建统筹 Agent
     */
    private OrchestratorAgent createOrchestratorAgent() {
        return AiServices.builder(OrchestratorAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(3))
                .build();
    }
}

