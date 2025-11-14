package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 酒店 Agent 工厂
 */
@Service
@Slf4j
public class HotelAgentFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    // Agent 缓存
    private static final Map<String, Object> agentCache = new ConcurrentHashMap<>();

    /**
     * 获取路由调度 Agent
     */
    public HotelRouterAgent getRouterAgent(String sessionId) {
        String cacheKey = sessionId + "_ROUTER";
        return (HotelRouterAgent) agentCache.computeIfAbsent(cacheKey, k -> createRouterAgent());
    }

    /**
     * 获取点餐 Agent
     */
    public MenuOrderAgent getMenuOrderAgent(String sessionId) {
        String cacheKey = sessionId + "_MENU_ORDER";
        return (MenuOrderAgent) agentCache.computeIfAbsent(cacheKey, k -> createMenuOrderAgent());
    }

    /**
     * 获取客房预订 Agent
     */
    public RoomBookingAgent getRoomBookingAgent(String sessionId) {
        String cacheKey = sessionId + "_ROOM_BOOKING";
        return (RoomBookingAgent) agentCache.computeIfAbsent(cacheKey, k -> createRoomBookingAgent());
    }

    /**
     * 获取叫醒服务 Agent
     */
    public WakeUpCallAgent getWakeUpCallAgent(String sessionId) {
        String cacheKey = sessionId + "_WAKE_UP_CALL";
        return (WakeUpCallAgent) agentCache.computeIfAbsent(cacheKey, k -> createWakeUpCallAgent());
    }

    /**
     * 获取礼宾服务 Agent
     */
    public ConciergeServiceAgent getConciergeServiceAgent(String sessionId) {
        String cacheKey = sessionId + "_CONCIERGE";
        return (ConciergeServiceAgent) agentCache.computeIfAbsent(cacheKey, k -> createConciergeServiceAgent());
    }

    /**
     * 获取汇总 Agent
     */
    public SummaryAgent getSummaryAgent(String sessionId) {
        String cacheKey = sessionId + "_SUMMARY";
        return (SummaryAgent) agentCache.computeIfAbsent(cacheKey, k -> createSummaryAgent());
    }

    /**
     * 获取续约 Agent
     */
    public RoomRenewalAgent getRoomRenewalAgent(String sessionId) {
        String cacheKey = sessionId + "_ROOM_RENEWAL";
        return (RoomRenewalAgent) agentCache.computeIfAbsent(cacheKey, k -> createRoomRenewalAgent());
    }

    /**
     * 获取支付 Agent
     */
    public PaymentAgent getPaymentAgent(String sessionId) {
        String cacheKey = sessionId + "_PAYMENT";
        return (PaymentAgent) agentCache.computeIfAbsent(cacheKey, k -> createPaymentAgent());
    }

    /**
     * 清理 session 的 Agent 缓存
     */
    public void clearAgentCache(String sessionId) {
        agentCache.remove(sessionId + "_ROUTER");
        agentCache.remove(sessionId + "_MENU_ORDER");
        agentCache.remove(sessionId + "_ROOM_BOOKING");
        agentCache.remove(sessionId + "_WAKE_UP_CALL");
        agentCache.remove(sessionId + "_CONCIERGE");
        agentCache.remove(sessionId + "_SUMMARY");
        agentCache.remove(sessionId + "_ROOM_RENEWAL");
        agentCache.remove(sessionId + "_PAYMENT");
        log.info("清理酒店 Agent 缓存: sessionId={}", sessionId);
    }

    private HotelRouterAgent createRouterAgent() {
        return AiServices.builder(HotelRouterAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    private MenuOrderAgent createMenuOrderAgent() {
        return AiServices.builder(MenuOrderAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private RoomBookingAgent createRoomBookingAgent() {
        return AiServices.builder(RoomBookingAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private WakeUpCallAgent createWakeUpCallAgent() {
        return AiServices.builder(WakeUpCallAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private ConciergeServiceAgent createConciergeServiceAgent() {
        return AiServices.builder(ConciergeServiceAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private SummaryAgent createSummaryAgent() {
        return AiServices.builder(SummaryAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private RoomRenewalAgent createRoomRenewalAgent() {
        return AiServices.builder(RoomRenewalAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private PaymentAgent createPaymentAgent() {
        return AiServices.builder(PaymentAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
