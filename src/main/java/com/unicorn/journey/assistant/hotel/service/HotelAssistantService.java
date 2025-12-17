package com.unicorn.journey.assistant.hotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.unicorn.journey.assistant.enums.VoiceCharacter;
import com.unicorn.journey.assistant.hotel.agent.*;
import com.unicorn.journey.assistant.hotel.entity.MenuItem;
import com.unicorn.journey.assistant.hotel.entity.MenuOrder;
import com.unicorn.journey.assistant.hotel.entity.SessionContext;
import com.unicorn.journey.assistant.hotel.entity.WakeUpAssistance;
import com.unicorn.journey.assistant.hotel.enums.SseEventType;
import com.unicorn.journey.assistant.hotel.factory.HotelAgentFactory;
import com.unicorn.journey.assistant.hotel.utils.SseEventSender;
import com.unicorn.journey.assistant.service.STTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 酒店助手服务
 */
@Slf4j
@Service
public class HotelAssistantService {

    private final HotelAgentFactory hotelAgentFactory;
    private final MenuService menuService;
    private final SseEventSender sseEventSender;
    private final STTService sttService;
    private final WakeUpSchedulerService wakeUpSchedulerService;

    // Agent实例
    private HotelRouterAgent hotelRouterAgent;
    private MOAgent moAgent;
    private WakeUpAgent wakeUpAgent;
    private SummaryAgent summaryAgent;
    private WakeUpCopywritingAgent wakeUpCopywritingAgent;
    private QueueTimeAgent queueTimeAgent;

    // 存储会话上下文（包含菜单、业务类型、SSE等所有会话数据）
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    // 存储订单
    private final Map<String, MenuOrder> orders = new ConcurrentHashMap<>();

    // 存储叫醒服务 key:wakeUpId
    private final Map<String, WakeUpAssistance> wakeUpAssistanceMap = new ConcurrentHashMap<>();

    //存储用户状态
    private final Map<String, String> userStatusMap = new ConcurrentHashMap<>();



    /**
     * 结构化数据包装类
     */
    private static class StructuredDataWrapper {
        private final String type;
        private final Object data;

        public StructuredDataWrapper(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    public HotelAssistantService(HotelAgentFactory hotelAgentFactory,
                                 MenuService menuService,
                                 SseEventSender sseEventSender,
                                 STTService sttService,
                                 WakeUpSchedulerService wakeUpSchedulerService) {
        this.hotelAgentFactory = hotelAgentFactory;
        this.menuService = menuService;
        this.sseEventSender = sseEventSender;
        this.sttService = sttService;
        this.wakeUpSchedulerService = wakeUpSchedulerService;
    }

    /**
     * 获取路由Agent
     */
    private HotelRouterAgent getRouterAgent() {
        if (hotelRouterAgent == null) {
            hotelRouterAgent = hotelAgentFactory.createRouterAgent();
        }
        return hotelRouterAgent;
    }

    /**
     * 获取点餐Agent
     */
    private MOAgent getMOAgent() {
        if (moAgent == null) {
            moAgent = hotelAgentFactory.createMOAgent();
        }
        return moAgent;
    }

    /**
     * 获取叫醒服务Agent
     */
    private WakeUpAgent getWakeUpAgent() {
        if (wakeUpAgent == null) {
            wakeUpAgent = hotelAgentFactory.createWakeUpAgent();
        }
        return wakeUpAgent;
    }

    /**
     * 获取等待时间查询服务Agent
     */
    private QueueTimeAgent getQueueTimeAgent() {
        if (queueTimeAgent == null) {
            queueTimeAgent = hotelAgentFactory.createQueueTimeAgent();
        }
        return queueTimeAgent;
    }

    /**
     * 获取叫醒文案生成Agent
     */
    private WakeUpCopywritingAgent getWakeUpCopywritingAgent() {
        if (wakeUpCopywritingAgent == null) {
            wakeUpCopywritingAgent = hotelAgentFactory.createWakeUpCopywritingAgent();
        }
        return wakeUpCopywritingAgent;
    }

    /**
     * 获取总结Agent
     */
    private SummaryAgent getSummaryAgent() {
        if (summaryAgent == null) {
            summaryAgent = hotelAgentFactory.createSummaryAgent();
        }
        return summaryAgent;
    }

    /**
     * 创建新会话并返回会话上下文
     */
    private SessionContext createSession(String sessionId, String userId) {
        // 创建SSE，60分钟超时
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.info("[SESSION] sessionId: {} 超时，移除SSE连接", sessionId);
            sessionContexts.remove(sessionId);
        });

        emitter.onCompletion(() -> {
            log.info("[SESSION] sessionId: {} 完成，移除SSE连接", sessionId);
            sessionContexts.remove(sessionId);
        });

        emitter.onError((e) -> {
            log.error("[SESSION] sessionId: {} 错误，移除SSE连接: {}", sessionId, e.getMessage());
            sessionContexts.remove(sessionId);
        });

        // 创建并存储会话上下文
        SessionContext context = SessionContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .sseEmitter(emitter)
                .lastDataType("")
                .build();

        sessionContexts.put(sessionId, context);
        log.info("[SESSION] 创建新会话: sessionId={}", sessionId);

        return context;
    }

    // ========================================================================
    // 主流程：处理用户消息
    // ========================================================================

    /**
     * 处理用户消息，返回SSE对象
     * <p>
     * 执行流程：
     * 1. 会话管理：创建/获取会话
     * 2. 任务规划：Router Agent 生成任务列表
     * 3. 意图解析：Router Agent 拆分用户消息
     * 4. Agent执行：根据任务类型调用对应的Agent
     * 5. 响应汇总：使用Summary Agent汇总
     * 6. 业务执行：根据特殊标记执行业务逻辑
     * 7. 数据清理：清除响应中的标记和JSON数据块
     * 8. SSE发送：发送文本、语音、结构化数据
     */
    public SseEmitter chat(String userId, String userMessage, String sessionId, boolean enableVoiceOutput, VoiceCharacter voiceCharacter) {
        // ===== 1：会话管理 =====
        SessionContext context;
        boolean isNewSession = false;

        // 如果sessionId为空或不存在，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session_" + userId + "_" + System.currentTimeMillis();
            context = createSession(sessionId, userId);
            isNewSession = true;
            log.info("[会话] 创建新会话: sessionId={}, userId={}", sessionId, userId);
        } else {
            context = sessionContexts.get(sessionId);
            if (context == null) {
                sessionId = "session_" + userId + "_" + System.currentTimeMillis();
                context = createSession(sessionId, userId);
                isNewSession = true;
                log.info("[会话] 会话已过期，创建新会话: sessionId={}, userId={}", sessionId, userId);
            } else {
                log.info("[会话] 使用已有会话: sessionId={}, userId={}", sessionId, userId);
            }
        }

        // 如果是新会话，先发送session_created事件
        final String finalSessionId = sessionId;
        final boolean finalIsNewSession = isNewSession;
        final SessionContext finalContext = context;
        final SseEmitter finalEmitter = context.getSseEmitter();

        Thread.startVirtualThread(() -> {
            try {
                // 如果是新会话，先发送sessionId
                if (finalIsNewSession) {
                    try {
                        Map<String, Object> sessionData = Map.of(
                                "sessionId", finalSessionId,
                                "userId", userId,
                                "createTime", System.currentTimeMillis()
                        );
                        finalEmitter.send(SseEmitter.event()
                                .name(SseEventType.SESSION_CREATED.getEventName())
                                .data(sessionData));
                        log.info("[SESSION] 发送sessionId: {}", finalSessionId);
                    } catch (Exception e) {
                        log.error("[SESSION] 发送sessionId失败: {}", e.getMessage(), e);
                    }
                }

                String memoryId = "hotel_" + finalSessionId;

                // ===== 2：任务规划 =====
                // 先通过路由Agent分析任务
                log.info("sessionId: {}, 用户消息: {}", finalSessionId, userMessage);

                // 获取业务上下文（使用SessionContext中的方法）
                String businessContext = finalContext.getBusinessContextString();
                
                // 添加用户状态到上下文中
                String userStatus = userStatusMap.getOrDefault(userId, "IN_ROOM"); // 默认IN_ROOM
                if (!businessContext.isEmpty()) {
                    businessContext += ", USER_STATUS=" + userStatus;
                } else {
                    businessContext = "USER_STATUS=" + userStatus;
                }
                
                if (!businessContext.isEmpty()) {
                    log.info("[上下文] {}", businessContext);
                }

                // 首先检测多个意图 用逗号分隔
                String tasks = getRouterAgent().generateTasks(memoryId + "_plan", userMessage, businessContext).trim();
                log.info("sessionId: {}, 多意图检测: {}", finalSessionId, tasks);

                // 记录上次执行的Agent类型
                if (tasks.equals("MO_AGENT")) {
                    finalContext.updateLastAgentType("MO_AGENT");
                } else if (tasks.equals("WAKEUP_AGENT")) {
                    finalContext.updateLastAgentType("WAKEUP_AGENT");
                } else if (tasks.equals("QUEUE_TIME_AGENT")) {
                    finalContext.updateLastAgentType("QUEUE_TIME_AGENT");
                } else if (tasks.equals("ROUTER_AGENT")) {
                    finalContext.updateLastAgentType("ROUTER_AGENT");
                } else if (tasks.contains(",")) {
                    finalContext.updateLastAgentType("");
                }

                // ===== 3：意图解析 =====
                // 解析用户意图（仅多任务时需要解析）
                Map<String, String> agentMessages = new HashMap<>();
                if (tasks.contains(",")) {
                    // 多个任务 - 需要解析用户意图，拆分消息
                    try {
                        String parsedJson = getRouterAgent().parseUserIntents(memoryId + "_parse", userMessage).trim();
                        log.info("[意图解析] 结果: {}", parsedJson);
                        ObjectMapper mapper = new ObjectMapper();
                        agentMessages = mapper.readValue(parsedJson,
                                mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
                    } catch (Exception e) {
                        log.warn("[意图解析] 意图解析失败~，使用原始消息: {}", e.getMessage());
                        if (tasks.contains("MO_AGENT")) agentMessages.put("MO_AGENT", userMessage);
                        if (tasks.contains("WAKEUP_AGENT")) agentMessages.put("WAKEUP_AGENT", userMessage);
                        if (tasks.contains("QUEUE_TIME_AGENT")) agentMessages.put("QUEUE_TIME_AGENT", userMessage);
                    }
                } else if (tasks.equals("MO_AGENT")) {
                    // 单任务 - 直接使用原始消息
                    agentMessages.put("MO_AGENT", userMessage);
                } else if (tasks.equals("WAKEUP_AGENT")) {
                    // 单任务 - 直接使用原始消息
                    agentMessages.put("WAKEUP_AGENT", userMessage);
                } else if (tasks.equals("QUEUE_TIME_AGENT")) {
                    // 单任务 - 直接使用原始消息
                    agentMessages.put("QUEUE_TIME_AGENT", userMessage);
                }

                String response;

                // ===== 4：agent 执行 =====
                // ===== 5：响应汇总 =====
                // ===== 6：业务执行 =====
                // ===== 7：数据清理 =====
                // 根据任务类型动态执行
                Map<String, String> agentResponses = new HashMap<>();
                List<StructuredDataWrapper> structuredDataList = new ArrayList<>();

                if (tasks.equals("ROUTER_AGENT")) {
                    // 检查是否是因为状态不匹配导致的ROUTER_AGENT
                    response = getDefaultResponseByCharacter(voiceCharacter);
                } else if (tasks.contains(",")) {
                    // 多个任务 - 并发执行
                    StopWatch agentWatch = new StopWatch("多任务Agent执行");
                    agentWatch.start("Agent执行");

                    String[] taskArray = tasks.split(",");
                    for (String task : taskArray) {
                        task = task.trim();
                        if ("MO_AGENT".equals(task)) {
                            String moMessage = agentMessages.getOrDefault("MO_AGENT", userMessage);
                            log.info("[Agent执行] MO_AGENT 使用消息: {}", moMessage);

                            StopWatch moWatch = new StopWatch("MO_AGENT");
                            moWatch.start();
                            agentResponses.put("MO_AGENT", getMOAgent().chat(memoryId + "_mo", moMessage, formatMenuList()));
                            moWatch.stop();
                            log.info("[耗时统计] MO_AGENT 执行耗时: {} ms", moWatch.getTotalTimeMillis());
                        } else if ("WAKEUP_AGENT".equals(task)) {
                            String wakeUpMessage = agentMessages.getOrDefault("WAKEUP_AGENT", userMessage);
                            log.info("[Agent执行] WAKEUP_AGENT 使用消息: {}", wakeUpMessage);

                            StopWatch wakeupWatch = new StopWatch("WAKEUP_AGENT");
                            wakeupWatch.start();
                            agentResponses.put("WAKEUP_AGENT", getWakeUpAgent().chat(memoryId + "_wakeup", wakeUpMessage));
                            wakeupWatch.stop();
                            log.info("[耗时统计] WAKEUP_AGENT 执行耗时: {} ms", wakeupWatch.getTotalTimeMillis());
                        } else if ("QUEUE_TIME_AGENT".equals(task)) {
                            String queueTimeMessage = agentMessages.getOrDefault("QUEUE_TIME_AGENT", userMessage);
                            log.info("[Agent执行] QUEUE_TIME_AGENT 使用消息: {}", queueTimeMessage);

                            StopWatch queueTimeWatch = new StopWatch("QUEUE_TIME_AGENT");
                            queueTimeWatch.start();
                            agentResponses.put("QUEUE_TIME_AGENT", getQueueTimeAgent().chat(memoryId + "_queuetime", queueTimeMessage));
                            queueTimeWatch.stop();
                            log.info("[耗时统计] QUEUE_TIME_AGENT 执行耗时: {} ms", queueTimeWatch.getTotalTimeMillis());
                        }
                    }
                    agentWatch.stop();
                    log.info("[耗时统计] 所有Agent执行总耗时: {} ms", agentWatch.getTotalTimeMillis());

                    // 汇总响应
                    StopWatch summaryWatch = new StopWatch("SummaryAgent");
                    summaryWatch.start();
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry : agentResponses.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n\n");
                    }
                    response = getSummaryAgent().summarizeResponses(sb.toString());
                    summaryWatch.stop();
                    log.info("[耗时统计] SummaryAgent 汇总耗时: {} ms", summaryWatch.getTotalTimeMillis());
                    log.info("[响应汇总] 多任务汇总完成, response:{}", response);

                    // 执行业务并收集结构化数据
                    structuredDataList = executeBusinessLogic(agentResponses, finalSessionId, finalContext, voiceCharacter);

                    // 清理标记
                    response = cleanAllDataTags(response);
                } else if (tasks.equals("MO_AGENT")) {
                    String moMessage = agentMessages.getOrDefault("MO_AGENT", userMessage);
                    log.info("[Agent执行] MO_AGENT 使用消息: {}", moMessage);

                    StopWatch moWatch = new StopWatch("MO_AGENT");
                    moWatch.start();
                    String moResponse = getMOAgent().chat(memoryId + "_mo", moMessage, formatMenuList());
                    moWatch.stop();
                    log.info("[耗时统计] MO_AGENT 执行耗时: {} ms", moWatch.getTotalTimeMillis());

                    agentResponses.put("MO_AGENT", moResponse);

                    // 汇总响应
                    StopWatch summaryWatch = new StopWatch("SummaryAgent");
                    summaryWatch.start();
                    response = getSummaryAgent().summarizeResponses("MO_AGENT: " + moResponse);
                    summaryWatch.stop();
                    log.info("[耗时统计] SummaryAgent 汇总耗时: {} ms", summaryWatch.getTotalTimeMillis());
                    log.info("[响应汇总] 单任务MO_AGENT汇总完成");

                    // 执行业务并收集结构化数据
                    structuredDataList = executeBusinessLogic(agentResponses, finalSessionId, finalContext, voiceCharacter);

                    // 清理标记
                    response = cleanAllDataTags(response);
                } else if (tasks.equals("WAKEUP_AGENT")) {
                    String wakeUpMessage = agentMessages.getOrDefault("WAKEUP_AGENT", userMessage);
                    log.info("[Agent执行] WAKEUP_AGENT 使用消息: {}", wakeUpMessage);

                    StopWatch wakeupWatch = new StopWatch("WAKEUP_AGENT");
                    wakeupWatch.start();
                    String wakeUpResponse = getWakeUpAgent().chat(memoryId + "_wakeup", wakeUpMessage);
                    wakeupWatch.stop();
                    log.info("[耗时统计] WAKEUP_AGENT 执行耗时: {} ms", wakeupWatch.getTotalTimeMillis());

                    agentResponses.put("WAKEUP_AGENT", wakeUpResponse);

                    // 汇总响应
                    StopWatch summaryWatch = new StopWatch("SummaryAgent");
                    summaryWatch.start();
                    response = getSummaryAgent().summarizeResponses("WAKEUP_AGENT: " + wakeUpResponse);
                    summaryWatch.stop();
                    log.info("[耗时统计] SummaryAgent 汇总耗时: {} ms", summaryWatch.getTotalTimeMillis());
                    log.info("[响应汇总] 单任务WAKEUP_AGENT汇总完成");

                    // 执行业务并收集结构化数据
                    structuredDataList = executeBusinessLogic(agentResponses, finalSessionId, finalContext, voiceCharacter);

                    // 清理标记
                    response = cleanAllDataTags(response);
                } else if (tasks.equals("QUEUE_TIME_AGENT")) {
                    String queueTimeMessage = agentMessages.getOrDefault("QUEUE_TIME_AGENT", userMessage);
                    log.info("[Agent执行] QUEUE_TIME_AGENT 使用消息: {}", queueTimeMessage);

                    StopWatch queueTimeWatch = new StopWatch("QUEUE_TIME_AGENT");
                    queueTimeWatch.start();
                    String queueTimeResponse = getQueueTimeAgent().chat(memoryId + "_queuetime", queueTimeMessage);
                    queueTimeWatch.stop();
                    log.info("[耗时统计] QUEUE_TIME_AGENT 执行耗时: {} ms", queueTimeWatch.getTotalTimeMillis());

                    agentResponses.put("QUEUE_TIME_AGENT", queueTimeResponse);

                    // 汇总响应
                    StopWatch summaryWatch = new StopWatch("SummaryAgent");
                    summaryWatch.start();
                    response = getSummaryAgent().summarizeResponses("QUEUE_TIME_AGENT: " + queueTimeResponse);
                    summaryWatch.stop();
                    log.info("[耗时统计] SummaryAgent 汇总耗时: {} ms", summaryWatch.getTotalTimeMillis());
                    log.info("[响应汇总] 单任务QUEUE_TIME_AGENT汇总完成");

                    // QUEUE_TIME_AGENT 不需要执行业务逻辑，直接返回响应
                    // 清理标记
                    response = cleanAllDataTags(response);
                } else {
                    response = getDefaultResponseByCharacter(voiceCharacter);
                }

                // ===== 8：SSE发送 =====
                // 如果启用语音输出，将文本转换为语音并发送
                if (enableVoiceOutput && !response.isEmpty()) {
                    try {
                        StopWatch voiceWatch = new StopWatch("语音生成");
                        voiceWatch.start();
                        log.info("[VOICE] 开始生成语音 - 角色: {}, 文本: {}",
                                voiceCharacter.name(),
                                response);
                        String audioPath = sttService.textToSpeechAndSave(response.replace("*", "").replace("#", ""), voiceCharacter);
                        voiceWatch.stop();
                        log.info("[耗时统计] 语音生成耗时: {} ms", voiceWatch.getTotalTimeMillis());
                        log.info("[VOICE] 语音生成成功 - 角色: {}, 路径: {}", voiceCharacter.name(), audioPath);

                        // 先发送语音消息
                        sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.VOICE.getEventName(),
                                Map.of(
                                        "audioPath", audioPath,
                                        "text", response,
                                        "type", "assistant",
                                        "voiceCharacter", voiceCharacter.name()
                                ));
                    } catch (Exception e) {
                        log.error("[VOICE] 语音生成失败: {}", e.getMessage(), e);
                        // 语音生成失败不影响主流程，继续处理
                    }
                }

                // 发送文字消息
                if (!response.isEmpty()) {
                    sseEventSender.sendEvent(finalSessionId, finalEmitter,
                            SseEventType.MESSAGE.getEventName(),
                            Map.of("content", response));
                    log.info("[MESSAGE] 发送文字消息: {}", response.substring(0, Math.min(50, response.length())));
                }

                // 发送所有结构化数据
                if (structuredDataList != null && !structuredDataList.isEmpty()) {
                    for (StructuredDataWrapper wrapper : structuredDataList) {
                        String dataType = wrapper.getType();
                        Object data = wrapper.getData();

                        // 更新会话上下文
                        if (dataType.equals("MENU") || dataType.equals("CONFIRM")) {
                            finalContext.updateLastDataType("MENU");
                        } else if (dataType.equals("WAKEUP")) {
                            finalContext.updateLastDataType("WAKEUP");
                        } else if (dataType.equals("ORDER")) {
                            finalContext.updateLastDataType("");
                        }

                        log.info("[SSE] sessionId: {}, 发送结构化数据，类型: {}", finalSessionId, dataType);
                        switch (dataType) {
                            case "MENU" -> {
                                sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.STRUCTURED_DATA.getEventName(),
                                        Map.of("type", "MENU", "data", data));
                            }
                            case "CONFIRM" -> {
                                sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.CONFIRM_MENU.getEventName(),
                                        Map.of("type", "CONFIRM", "data", data));
                            }
                            case "ORDER" -> {
                                sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.STRUCTURED_DATA.getEventName(),
                                        Map.of("type", "ORDER", "data", data));
                            }
                            case "TIME_INPUT" -> {
                                sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.STRUCTURED_DATA.getEventName(),
                                        Map.of("type", "TIME_INPUT", "data", data));
                            }
                            case "STORY" -> {
                                // 处理讲故事：生成语音并发送voice事件
                                if (data instanceof Map storyData) {
                                    String storyText = (String) storyData.get("storyText");
                                    if (storyText != null && !storyText.isEmpty()) {
                                        try {
                                            log.info("[STORY-VOICE] 开始生成故事语音");
                                            StopWatch storyVoiceWatch = new StopWatch("故事语音生成");
                                            storyVoiceWatch.start();
                                            String audioPath = sttService.textToSpeechAndSave(storyText, voiceCharacter);
                                            storyVoiceWatch.stop();
                                            log.info("[耗时统计] 故事语音生成耗时: {} ms", storyVoiceWatch.getTotalTimeMillis());
                                            log.info("[STORY-VOICE] 故事语音生成成功: {}", audioPath);

                                            // 发送故事语音事件（与叫醒服务统一格式）
                                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                                    SseEventType.VOICE.getEventName(),
                                                    Map.of(
                                                            "audioPath", audioPath,
                                                            "text", storyText,
                                                            "type", "story",
                                                            "voiceCharacter", voiceCharacter.name()
                                                    ));
                                        } catch (Exception e) {
                                            log.error("[STORY-VOICE] 故事语音生成失败: {}", e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                            case "WAKEUP" -> {
                                sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.STRUCTURED_DATA.getEventName(),
                                        Map.of("type", "WAKE_UP", "data", data));

                                // 如果有语音路径，也发送语音事件
                                if (data instanceof WakeUpAssistance wakeUp) {
                                    String remark = wakeUp.getRemark();
                                    if (remark != null && remark.startsWith("audioPath:")) {
                                        String audioPath = remark.substring("audioPath:".length());
                                        log.info("[WAKEUP-VOICE] 发送陪伴服务语音: {}", audioPath);

                                        String voiceText = generateWakeUpText(wakeUp);

                                        sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                                SseEventType.VOICE.getEventName(),
                                                Map.of(
                                                        "audioPath", audioPath,
                                                        "text", voiceText,
                                                        "type", "wakeup_confirmation",
                                                        "timeInterval", "100"
                                                ));

                                        wakeUp.setRemark("酒店叫醒服务");
                                    }
                                }
                            }
                        }
                    }
                }

                log.info("[SSE] sessionId: {} 消息处理完成，保持连接", finalSessionId);

            } catch (Exception e) {
                log.error("[SSE] sessionId: {}, 处理消息失败: {}", finalSessionId, e.getMessage(), e);
                sseEventSender.sendError(finalSessionId, finalEmitter, e.getMessage());
            }
        });

        return finalEmitter;
    }

    /**
     * 格式化菜单列表为字符串
     */
    private String formatMenuList() {
        List<MenuItem> allItems = menuService.getAllMenuItems();
        StringBuilder sb = new StringBuilder();
        sb.append("所有可用菜品：\n\n");

        // 按类型分组
        Map<String, List<MenuItem>> groupedByCategory = allItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(MenuItem::getCategory));

        groupedByCategory.forEach((category, items) -> {
            sb.append("**").append(category).append("菜品**\n");
            for (MenuItem item : items) {
                sb.append(String.format("%d. %s - %d元（%s）\n",
                        item.getMenuId(), item.getName(), item.getPrice(),
                        item.getFlavors()));
            }
            sb.append("\n");
        });

        return sb.toString();
    }

    /**
     * 清理响应中的JSON数据块
     * 移除 [MENU_DATA]{...}[/MENU_DATA] 和 [SELECTED_DATA]{...}[/SELECTED_DATA]
     */
    private String cleanJSONDataBlocks(String response) {
        // 清理MENU_DATA数据块
        response = response.replaceAll("\\[MENU_DATA\\].*?\\[/MENU_DATA\\]", "").trim();
        // 清理SELECTED_DATA数据块
        response = response.replaceAll("\\[SELECTED_DATA\\].*?\\[/SELECTED_DATA\\]", "").trim();

        // 清理多余的空行（连续的\n\n或更多）
        response = response.replaceAll("(\\n\\s*){3,}", "\n\n").trim();

        return response;
    }

    /**
     * 从响应中提取菜单
     * 优先级：JSON结构化数据 > 正则文本提取
     */
    private List<MenuItem> extractMenuFromResponse(String response) {
        List<MenuItem> menuItems = new ArrayList<>();

        log.info("[MENU] 开始提取菜单");

        // 第一步：尝试从JSON数据中提取
        List<MenuItem> jsonMenuItems = tryExtractMenuFromJSON(response);
        if (!jsonMenuItems.isEmpty()) {
            log.info("[MENU] 从JSON成功提取 {} 个菜品", jsonMenuItems.size());
            return jsonMenuItems;
        }

        // 第二步：降级使用正则提取
        log.warn("[MENU] JSON提取失败，使用正则表达式降级提取");
        Pattern pattern = Pattern.compile("(?:^|\\n)\\s*(?:\\d+\\.)?\\s*\\*{0,2}\\s*([^−\\-*\\n（(]+?)\\s*\\*{0,2}\\s*[−\\-]\\s*([\\d.]+)元", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String dishName = matcher.group(1).trim();
            if (dishName.isEmpty() || dishName.equals("-")) {
                continue;
            }

            MenuItem item = menuService.getMenuItemByName(dishName);
            if (item != null) {
                menuItems.add(item);
                log.info("[MENU] 从文本提取菜品: {}", item.getName());
            }
        }

        log.info("[MENU] 提取完成，共提取 {} 个菜品", menuItems.size());
        return menuItems;
    }

    /**
     * 从JSON数据中尝试提取菜单
     * 格式：[MENU_DATA]{"items":[{"id":1,"name":"菜品名","price":价格},...]}[/MENU_DATA]
     */
    private List<MenuItem> tryExtractMenuFromJSON(String response) {
        List<MenuItem> menuItems = new ArrayList<>();

        try {
            Pattern jsonPattern = Pattern.compile("\\[MENU_DATA\\]\\s*\\{(.+?)\\}\\s*\\[/MENU_DATA\\]", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(response);

            if (!jsonMatcher.find()) {
                return menuItems;
            }

            String jsonContent = "{" + jsonMatcher.group(1) + "}";
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonNode = mapper.readValue(jsonContent, ObjectNode.class);

            if (jsonNode.has("items") && jsonNode.get("items").isArray()) {
                jsonNode.get("items").forEach(item -> {
                    try {
                        String name = item.get("name").asText();
                        MenuItem menuItem = menuService.getMenuItemByName(name);
                        if (menuItem != null) {
                            menuItems.add(menuItem);
                            log.debug("[MENU] 从JSON提取菜品: {}", name);
                        }
                    } catch (Exception e) {
                        log.debug("[MENU] 解析JSON菜品项失败");
                    }
                });
            }
        } catch (Exception e) {
            log.debug("[MENU] JSON解析失败：{}", e.getMessage());
        }

        return menuItems;
    }

    /**
     * 从确认响应中提取用户已选择的菜品
     * 优先级：JSON结构化数据 > 正则文本提取
     */
    private List<MenuItem> extractSelectedMenuFromResponse(String response, SessionContext context) {
        List<MenuItem> selectedItems = new ArrayList<>();

        log.info("[CONFIRM] 开始提取已选菜品");

        // 第一步：尝试从JSON数据中提取
        List<MenuItem> jsonItems = tryExtractSelectedFromJSON(response);
        if (!jsonItems.isEmpty()) {
            log.info("[CONFIRM] 从JSON成功提取 {} 个已选菜品", jsonItems.size());
            context.setSelectedMenuItems(jsonItems);
            return jsonItems;
        }

        // 第二步：降级使用正则提取
        log.warn("[CONFIRM] JSON提取失败，使用正则表达式降级提取");
        Pattern pattern = Pattern.compile("(?:^|\\n)\\s*(?:\\d+\\.)?\\s*([^−\\-*\\n（(]+?)\\s*[−\\-]\\s*([\\d.]+)元", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String dishName = matcher.group(1).trim();
            if (dishName.isEmpty() || dishName.equals("-")) {
                continue;
            }

            MenuItem item = menuService.getMenuItemByName(dishName);
            if (item != null) {
                selectedItems.add(item);
                log.info("[CONFIRM] 从文本提取已选菜品: {}", item.getName());
            }
        }

        log.info("[CONFIRM] 提取完成，共提取 {} 个已选菜品", selectedItems.size());

        // 存储当前会话的已选菜品到SessionContext中
        context.setSelectedMenuItems(selectedItems);

        return selectedItems;
    }

    /**
     * 从JSON数据中尝试提取已选菜品
     * 格式：[SELECTED_DATA]{"items":[{"id":1,"name":"菜品名","price":价格},...],"total":总价}[/SELECTED_DATA]
     */
    private List<MenuItem> tryExtractSelectedFromJSON(String response) {
        List<MenuItem> selectedItems = new ArrayList<>();

        try {
            Pattern jsonPattern = Pattern.compile("\\[SELECTED_DATA\\]\\s*\\{(.+?)\\}\\s*\\[/SELECTED_DATA\\]", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(response);

            if (!jsonMatcher.find()) {
                return selectedItems;
            }

            String jsonContent = "{" + jsonMatcher.group(1) + "}";
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonNode = mapper.readValue(jsonContent, ObjectNode.class);

            if (jsonNode.has("items") && jsonNode.get("items").isArray()) {
                jsonNode.get("items").forEach(item -> {
                    try {
                        String name = item.get("name").asText();
                        MenuItem menuItem = menuService.getMenuItemByName(name);
                        if (menuItem != null) {
                            selectedItems.add(menuItem);
                            log.debug("[CONFIRM] 从JSON提取已选菜品: {}", name);
                        }
                    } catch (Exception e) {
                        log.debug("[CONFIRM] 解析JSON菜品项失败");
                    }
                });
            }
        } catch (Exception e) {
            log.debug("[CONFIRM] JSON解析失败：{}", e.getMessage());
        }

        return selectedItems;
    }

    /**
     * 生成订单
     */
    private MenuOrder generateOrder(String userId, SessionContext context) {
        List<MenuItem> items = context.getSelectedMenuItems() != null ? context.getSelectedMenuItems() : new ArrayList<>();

        double totalPrice = items.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();

        MenuOrder order = MenuOrder.builder()
                .orderId("ORDER_" + System.currentTimeMillis())
                .userId(userId)
                .items(items)
                .totalPrice(totalPrice)
                .createTime(LocalDateTime.now())
                .status("CONFIRMED")
                .build();

        orders.put(order.getOrderId(), order);
        log.info("生成订单: {}", order.getOrderId());

        return order;
    }

    /**
     * 生成叫醒服务
     */
    private WakeUpAssistance generateWakeUpAssistance(String userId, String response, String sessionId, SseEmitter emitter, VoiceCharacter voiceCharacter) {
        log.info("[WAKEUP] 开始生成叫醒服务，响应内容: {}", response);

        // 提取叫醒时间
        LocalDateTime wakeUpTime = extractTimeFromResponse(response);

        if (wakeUpTime == null) {
            log.error("[WAKEUP] 无法从响应中提取时间");
            wakeUpTime = LocalDateTime.now().plusDays(1).withHour(7).withMinute(0).withSecond(0).withNano(0);
        }

        // 先创建临时的叫醒服务对象（用于生成语音文案）
        WakeUpAssistance tempWakeUp = WakeUpAssistance.builder()
                .wakeUpId("WAKEUP_" + System.currentTimeMillis())
                .userId(userId)
                .sessionId(sessionId)
                .wakeUpTime(wakeUpTime)
                .createTime(LocalDateTime.now())
                .status("PENDING")
                .remark("酒店叫醒服务")
                .build();

        // 生成叫醒语音文案
        StopWatch copyWatch = new StopWatch("叫醒文案生成");
        copyWatch.start();
        String voiceText = generateWakeUpVoiceText(tempWakeUp);
        copyWatch.stop();
        log.info("[耗时统计] 叫醒文案生成耗时: {} ms", copyWatch.getTotalTimeMillis());

        // 生成语音文件（使用用户选择的语音角色）
        String voicePath = null;
        try {
            StopWatch voiceWatch = new StopWatch("叫醒语音生成");
            voiceWatch.start();
            voicePath = sttService.textToSpeechAndSave(voiceText, voiceCharacter);
            voiceWatch.stop();
            log.info("[耗时统计] 叫醒语音生成耗时: {} ms", voiceWatch.getTotalTimeMillis());
            log.info("[WAKEUP] 语音文件生成成功 - 角色: {}, 路径: {}", voiceCharacter.name(), voicePath);
        } catch (Exception e) {
            log.error("[WAKEUP] 语音文件生成失败: {}", e.getMessage(), e);
        }

        // 创建最终的叫醒服务对象（包含语音路径）
        WakeUpAssistance wakeUpAssistance = WakeUpAssistance.builder()
                .wakeUpId(tempWakeUp.getWakeUpId())
                .userId(userId)
                .sessionId(sessionId)
                .wakeUpTime(wakeUpTime)
                .createTime(tempWakeUp.getCreateTime())
                .status("PENDING")
                .voicePath(voicePath)
                .remark("酒店叫醒服务")
                .build();

        wakeUpAssistanceMap.put(wakeUpAssistance.getWakeUpId(), wakeUpAssistance);
        log.info("[WAKEUP] 生成叫醒服务: {}, 时间: {}, sessionId: {}, 语音路径: {}",
                wakeUpAssistance.getWakeUpId(), wakeUpTime, sessionId, voicePath);

        // 调度叫醒任务
        wakeUpSchedulerService.scheduleWakeUp(wakeUpAssistance, emitter);

        return wakeUpAssistance;
    }

    /**
     * 生成叫醒语音文本
     */
    private String generateWakeUpVoiceText(WakeUpAssistance wakeUp) {
        LocalDateTime wakeUpTime = wakeUp.getWakeUpTime();

        // 普通叫醒：使用简单的音乐式叫醒，不讲故事
        log.info("[WAKEUP] 普通叫醒服务，生成简单叫醒提示");
        return generateWakeUpMusicText(wakeUpTime);
    }

    /**
     * 生成叫醒文案
     */
    private String generateWakeUpMusicText(LocalDateTime wakeUpTime) {

        wakeUpCopywritingAgent = hotelAgentFactory.createWakeUpCopywritingAgentWithHighTemperature();

        String response = wakeUpCopywritingAgent.generateWakeUpCopy(wakeUpTime.toString());

        return response;

//        int hour = wakeUpTime.getHour();
//        int minute = wakeUpTime.getMinute();
//
//
//        // 根据时间段生成不同的问候
//        String timeGreeting;
//        String musicGreeting;
//
//
//        if (hour >= 5 && hour < 9) {
//            timeGreeting = "早上好";
//            musicGreeting = "早上的阳光多美好！";
//        } else if (hour >= 9 && hour < 12) {
//            timeGreeting = "上午好";
//            musicGreeting = "新的一天开始啦！";
//        } else if (hour >= 12 && hour < 14) {
//            timeGreeting = "中午好";
//            musicGreeting = "该起床啦！";
//        } else if (hour >= 14 && hour < 18) {
//            timeGreeting = "下午好";
//            musicGreeting = "下午的时光不要费！";
//        } else {
//            timeGreeting = "晚上好";
//            musicGreeting = "该起床啦！";
//        }
//
//
//        String minuteText = minute == 0 ? "" : String.format("%d分", minute);
//
//
//        String response = String.format(
//                        "尊敬的客人，%s！\n" +
//                        "现在是%d点%s，%s \n" +
//                        "祝您今天有个美好的一天！",
//                musicGreeting,
//                timeGreeting,
//                hour,
//                minuteText,
//                "该起床啦"
//        );
//        // 生成固定叫醒文案
//        return response;
    }


    /**
     * 生成叫醒服务文案
     */
    private String generateWakeUpText(WakeUpAssistance wakeUp) {
        LocalDateTime wakeUpTime = wakeUp.getWakeUpTime();

        // 格式化时间
        String timeStr = String.format("%d月%d日 %02d:%02d",
                wakeUpTime.getMonthValue(),
                wakeUpTime.getDayOfMonth(),
                wakeUpTime.getHour(),
                wakeUpTime.getMinute());

        // 生成专业的确认文案
        return String.format(
                "尊敬的客人，您好！\n\n" +
                        "您的叫醒服务已成功执行。\n" +
                        "叫醒时间：%s\n" +
                        "服务编号：%s\n\n" +
                        "祝您住店愉快，睡眠充足！",
                timeStr,
                wakeUp.getWakeUpId()
        );
    }

    /**
     * 从响应中提取时间
     * 支持格式：
     * - "明天早上7点" / "明天上午7点"
     * - "07:00" / "7:00"
     * - "早上7点" / "上午7点半"
     */
    private LocalDateTime extractTimeFromResponse(String response) {
        LocalDateTime now = LocalDateTime.now();

        // 尝试匹配 HH:mm 格式
        Pattern timePattern = Pattern.compile("(\\d{1,2})[:：](\\d{2})");
        Matcher timeMatcher = timePattern.matcher(response);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = Integer.parseInt(timeMatcher.group(2));

            // 判断是今天还是明天
            LocalDateTime result = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            if (result.isBefore(now)) {
                result = result.plusDays(1);
            }

            log.info("[WAKEUP] 提取时间成功: {}", result);
            return result;
        }

        // 尝试匹配中文时间表达（早上X点、上午X点半）
        Pattern chinesePattern = Pattern.compile("(早上|上午|中午|下午|晚上).*?(\\d{1,2})点(半|\\d{2}分)?");
        Matcher chineseMatcher = chinesePattern.matcher(response);
        if (chineseMatcher.find()) {
            String period = chineseMatcher.group(1);
            int hour = Integer.parseInt(chineseMatcher.group(2));
            String minutePart = chineseMatcher.group(3);

            // 转换为24小时制
            if (("下午".equals(period) || "晚上".equals(period)) && hour < 12) {
                hour += 12;
            }

            int minute = 0;
            if ("半".equals(minutePart)) {
                minute = 30;
            } else if (minutePart != null && minutePart.endsWith("分")) {
                minute = Integer.parseInt(minutePart.replace("分", ""));
            }

            LocalDateTime result = now.plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            log.info("[WAKEUP] 提取时间成功: {}", result);
            return result;
        }

        log.warn("[WAKEUP] 无法提取时间，使用默认值（明天7:00）");
        return null;
    }

    /**
     * 获取订单详情
     */
    public MenuOrder getOrder(String orderId) {
        return orders.get(orderId);
    }

    /**
     * 获取用户所有订单
     */
    public List<MenuOrder> getUserOrders(String userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .toList();
    }

    /**
     * 获取叫醒服务详情
     */
    public WakeUpAssistance getWakeUpAssistance(String wakeUpId) {
        return wakeUpAssistanceMap.get(wakeUpId);
    }

    /**
     * 获取用户所有叫醒服务
     */
    public List<WakeUpAssistance> getUserWakeUpAssistanceMap(String userId) {
        return wakeUpAssistanceMap.values().stream()
                .filter(service -> service.getUserId().equals(userId))
                .toList();
    }

    /**
     * 删除叫醒服务
     *
     * @param wakeUpId 叫醒服务ID
     * @return 是否删除成功
     */
    public boolean deleteWakeUpService(String wakeUpId) {
        WakeUpAssistance removed = wakeUpAssistanceMap.remove(wakeUpId);
        if (removed != null) {
            // 取消定时任务
            wakeUpSchedulerService.cancelWakeUp(wakeUpId);

            log.info("[WAKEUP] 删除叫醒服务成功: {}, 时间: {}", wakeUpId, removed.getWakeUpTime());
            return true;
        } else {
            log.warn("[WAKEUP] 叫醒服务不存在: {}", wakeUpId);
            return false;
        }
    }

    /**
     * 清除会话数据
     */
    public void clearSession(String sessionId) {
        SessionContext context = sessionContexts.remove(sessionId);
        if (context != null) {
            context.clear();
            log.info("[SESSION] 清除会话数据: sessionId={}", sessionId);
        }
        hotelRouterAgent = null;
        moAgent = null;
        wakeUpAgent = null;
        summaryAgent = null;
        wakeUpCopywritingAgent = null;
        queueTimeAgent = null;
    }

    /**
     * 执行业务逻辑 - 根据Agent输出中的特殊标记执行对应业务
     *
     * @param agentResponses Agent响应Map
     * @param sessionId      会话 ID
     * @param context        会话上下文
     * @param voiceCharacter 语音角色
     * @return 结构化数据列表
     */
    private List<StructuredDataWrapper> executeBusinessLogic(Map<String, String> agentResponses, String sessionId, SessionContext context, VoiceCharacter voiceCharacter) {
        List<StructuredDataWrapper> structuredDataList = new ArrayList<>();

        for (Map.Entry<String, String> entry : agentResponses.entrySet()) {
            String agentName = entry.getKey();
            String response = entry.getValue();

            try {
                if ("MO_AGENT".equals(agentName)) {
                    // 处理点餐业务
                    if (response.contains("[SHOW_MENU]")) {
                        // 展示菜单列表
                        log.info("[BUSINESS] 展示菜单");
                        List<MenuItem> menuItems = extractMenuFromResponse(response);
                        if (!menuItems.isEmpty()) {
                            structuredDataList.add(new StructuredDataWrapper("MENU", menuItems));
                            log.info("[BUSINESS] 提取到 {} 个菜品", menuItems.size());
                        }
                    } else if (response.contains("[CONFIRM_MENU]")) {
                        // 确认菜单
                        log.info("[BUSINESS] 确认菜单");
                        List<MenuItem> selectedItems = extractSelectedMenuFromResponse(response, context);
                        if (!selectedItems.isEmpty()) {
                            structuredDataList.add(new StructuredDataWrapper("CONFIRM", selectedItems));
                            log.info("[BUSINESS] 确认 {} 个菜品", selectedItems.size());
                        }
                    } else if (response.contains("[GENERATE_ORDER]")) {
                        // 生成订单
                        log.info("[BUSINESS] 执行MO业务: 生成订单");
                        List<MenuItem> selectedItems = tryExtractSelectedFromJSON(response);
                        if (selectedItems.isEmpty()) {
                            selectedItems = context.getSelectedMenuItems();
                        }

                        if (selectedItems != null && !selectedItems.isEmpty()) {
                            context.setSelectedMenuItems(selectedItems);
                            MenuOrder order = generateOrder(context.getUserId(), context);
                            log.info("[BUSINESS] 订单已生成: {}, 包含 {} 个菜品，总价: {}",
                                    order.getOrderId(), selectedItems.size(), order.getTotalPrice());
                            structuredDataList.add(new StructuredDataWrapper("ORDER", order));
                        } else {
                            log.warn("[BUSINESS] 无法提取菜品信息，订单生成失败");
                        }
                    }
                } else if ("WAKEUP_AGENT".equals(agentName)) {
                    // 处理叫醒业务
                    if (response.contains("[REQUEST_TIME_INPUT]")) {
                        // 请求用户输入时间
                        log.info("[BUSINESS] 请求用户输入叫醒时间");
                        structuredDataList.add(new StructuredDataWrapper("TIME_INPUT",
                                Map.of("message", "请选择叫醒时间")));
                    } else if (response.contains("[GENERATE_WAKEUP_AND_TELL_STORY]")) {
                        // 同时生成叫醒服务并讲故事
                        log.info("[BUSINESS] 执行双重服务: 生成叫醒 + 立即讲故事");
                        LocalDateTime wakeUpTime = extractTimeFromResponse(response);
                        if (wakeUpTime != null) {
                            String userId = context.getUserId();
                            SseEmitter emitter = context.getSseEmitter();

                            // 1. 生成叫醒服务
                            WakeUpAssistance wakeUpAssistance = generateWakeUpAssistance(userId, response, sessionId, emitter, voiceCharacter);
                            log.info("[BUSINESS] 已生成叫醒服务: {}", wakeUpAssistance.getWakeUpId());
                            structuredDataList.add(new StructuredDataWrapper("WAKEUP", wakeUpAssistance));

                            // 2. 立即讲故事（从响应中提取故事文本）
                            String storyText = extractStoryFromResponse(response);
                            if (storyText != null && !storyText.isEmpty()) {
                                log.info("[BUSINESS] 立即讲故事: {}", storyText.substring(0, Math.min(50, storyText.length())));
                                structuredDataList.add(new StructuredDataWrapper("STORY",
                                        Map.of(
                                                "text", storyText,
                                                "type", "story",
                                                "voiceCharacter", voiceCharacter.name()
                                        )));
                            }
                        }
                    } else if (response.contains("[GENERATE_WAKEUP]")) {
                        // 生成叫醒服务
                        LocalDateTime wakeUpTime = extractTimeFromResponse(response);
                        if (wakeUpTime != null) {
                            String userId = context.getUserId();
                            SseEmitter emitter = context.getSseEmitter();
                            WakeUpAssistance wakeUpAssistance = generateWakeUpAssistance(userId, response, sessionId, emitter, voiceCharacter);
                            log.info("[BUSINESS] 执行WAKEUP业务: 已生成叫醒服务: {}", wakeUpAssistance.getWakeUpId());
                            structuredDataList.add(new StructuredDataWrapper("WAKEUP", wakeUpAssistance));
                        }
                    } else if (response.contains("[TELL_STORY]")) {
                        // 立即讲故事（不需要设置叫醒）
                        log.info("[BUSINESS] 执行讲故事服务");
                        String storyText = extractStoryFromResponse(response);
                        if (storyText != null && !storyText.isEmpty()) {
                            log.info("[BUSINESS] 故事内容: {}", storyText.substring(0, Math.min(50, storyText.length())));
                            structuredDataList.add(new StructuredDataWrapper("STORY",
                                    Map.of(
                                            "text", storyText,
                                            "type", "story",
                                            "voiceCharacter", voiceCharacter.name()
                                    )));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[BUSINESS] 执行业务失败: {}", e.getMessage(), e);
            }
        }

        return structuredDataList;
    }

    /**
     * 从响应中提取故事文本
     */
    private String extractStoryFromResponse(String response) {
        try {
            // 查找故事标题后的内容
            if (response.contains("📖 迪士尼故事时间") || response.contains("迪士尼故事")) {
                // 提取 ### 📖 迪士尼故事时间 之后到标记之前的内容
                int storyStart = response.indexOf("📖");
                if (storyStart == -1) {
                    storyStart = response.indexOf("迪士尼故事");
                }

                if (storyStart != -1) {
                    // 找到最后一个标记的位置
                    int markerPos = response.lastIndexOf("[");
                    String storySection = markerPos > storyStart ?
                            response.substring(storyStart, markerPos) :
                            response.substring(storyStart);

                    // 清理 Markdown 格式和多余空格
                    storySection = storySection.replaceAll("###\\s*", "");
                    storySection = storySection.replaceAll("\\*\\*", "");
                    storySection = storySection.replaceAll("-{3,}", ""); // 移除分隔线
                    storySection = storySection.trim();

                    // 提取故事正文（跳过标题）
                    String[] lines = storySection.split("\\n");
                    StringBuilder story = new StringBuilder();
                    boolean foundStory = false;

                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        // 跳过标题和提示性文字
                        if (line.contains("迪士尼故事") ||
                                line.contains("讲一个") ||
                                line.contains("现在让我") ||
                                line.contains("记住：") ||
                                line.startsWith("📖")) {
                            foundStory = true;
                            continue;
                        }

                        if (foundStory && line.length() > 10) {
                            story.append(line).append(" ");
                        }
                    }

                    String result = story.toString().trim();
                    if (!result.isEmpty()) {
                        log.info("[故事提取] 成功提取故事: {}", result.substring(0, Math.min(50, result.length())));
                        return result;
                    }
                }
            }

            log.warn("[故事提取] 未找到故事内容");
            return null;
        } catch (Exception e) {
            log.error("[故事提取] 提取失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 清理所有数据标签（不推特殊标记给用户）
     */
    private String cleanAllDataTags(String response) {
        // 清理所有标记
        response = response.replace("[SHOW_MENU]", "").trim();
        response = response.replace("[CONFIRM_MENU]", "").trim();
        response = response.replace("[GENERATE_ORDER]", "").trim();
        response = response.replace("[REQUEST_TIME_INPUT]", "").trim();
        response = response.replace("[GENERATE_WAKEUP]", "").trim();
        response = response.replace("[TELL_STORY]", "").trim();
        response = response.replace("[GENERATE_WAKEUP_AND_TELL_STORY]", "").trim();

        // 清理JSON数据块
        response = cleanJSONDataBlocks(response);

        return response.trim();
    }

    /**
     * 根据语音角色获取默认响应文案
     */
    private String getDefaultResponseByCharacter(VoiceCharacter voiceCharacter) {
        return switch (voiceCharacter) {
            case JUDY ->
                    "你好呀～我是兔子警官朱迪，您的专属伴侣！🐰 我可以为您讲迪士尼故事、温柔地叫醒您，还能帮您订餐，做您的导游。请问您需要什么服务呢？";
            case NICK ->
                    "宝贝儿～我是尼克，您的贴心伴侣！🦊 我能给您讲迪士尼的精彩故事、提供叫醒服务，帮您点外卖，还能当您的专属导游。有什么需要尽管说！";
        };
    }


    public void switchStatus(String userId, String status) {
        //IN_ROOM or IN_PARK
        log.info("switchStatus: userId: {}, status: {}", userId, status);
        userStatusMap.put(userId, status);
    }
}
