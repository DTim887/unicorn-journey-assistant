package com.unicorn.journey.assistant.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.enums.VoiceCharacter;
import com.unicorn.journey.assistant.hotel.agent.QueueTimeAgent;
import com.unicorn.journey.assistant.hotel.agent.RoomWakeUpAgent;
import com.unicorn.journey.assistant.hotel.agent.TakeHomeAgent;
import com.unicorn.journey.assistant.hotel.entity.DpaOrder;
import com.unicorn.journey.assistant.hotel.entity.SessionContext;
import com.unicorn.journey.assistant.hotel.entity.WakeUpAssistance;
import com.unicorn.journey.assistant.hotel.enums.SseEventType;
import com.unicorn.journey.assistant.hotel.factory.HotelAgentFactory;
import com.unicorn.journey.assistant.hotel.utils.SseEventSender;
import com.unicorn.journey.assistant.service.STTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户状态路由服务
 * 根据用户状态(IN_PARK/IN_ROOM)路由到不同的服务
 */
@Slf4j
@Service
public class UserStatusRouterService {

    private final HotelAssistantService hotelAssistantService;
    private final SseEventSender sseEventSender;
    private final STTService sttService;
    private final HotelAgentFactory hotelAgentFactory;
    private final WakeUpSchedulerService wakeUpSchedulerService;

    // 存储会话上下文
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    // 用户对话记录 key: userId, value: 消息列表
    private final Map<String, List<String>> userConversationHistory = new ConcurrentHashMap<>();

    // 存储叫醒服务 key:wakeUpId
    private final Map<String, WakeUpAssistance> wakeUpAssistanceMap = new ConcurrentHashMap<>();
    
    // 存储DPA订单 key:dpaOrderNo
    private final Map<String, DpaOrder> dpaOrderMap = new ConcurrentHashMap<>();
    
    // 临时存储DPA产品信息（用于展示）key:sessionId
    private final Map<String, Map<String, Object>> tempDpaProductMap = new ConcurrentHashMap<>();

    // TakeHomeAgent
    private TakeHomeAgent takeHomeAgent;
    
    // RoomWakeUpAgent
    private RoomWakeUpAgent roomWakeUpAgent;
    
    // QueueTimeAgent - 园区内Agent
    private QueueTimeAgent queueTimeAgent;

    public UserStatusRouterService(HotelAssistantService hotelAssistantService,
                                   SseEventSender sseEventSender,
                                   STTService sttService,
                                   HotelAgentFactory hotelAgentFactory,
                                   WakeUpSchedulerService wakeUpSchedulerService) {
        this.hotelAssistantService = hotelAssistantService;
        this.sseEventSender = sseEventSender;
        this.sttService = sttService;
        this.hotelAgentFactory = hotelAgentFactory;
        this.wakeUpSchedulerService = wakeUpSchedulerService;
    }

    /**
     * 获取TakeHomeAgent
     */
    private TakeHomeAgent getTakeHomeAgent() {
        if (takeHomeAgent == null) {
            takeHomeAgent = hotelAgentFactory.createTakeHomeAgent();
        }
        return takeHomeAgent;
    }

    /**
     * 获取RoomWakeUpAgent
     */
    private RoomWakeUpAgent getRoomWakeUpAgent() {
        if (roomWakeUpAgent == null) {
            roomWakeUpAgent = hotelAgentFactory.createRoomWakeUpAgent();
        }
        return roomWakeUpAgent;
    }

    /**
     * 获取QueueTimeAgent - 园区内Agent
     */
    private QueueTimeAgent getQueueTimeAgent() {
        if (queueTimeAgent == null) {
            queueTimeAgent = hotelAgentFactory.createQueueTimeAgent();
        }
        return queueTimeAgent;
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

    /**
     * 根据用户状态路由聊天请求
     */
    public SseEmitter chat(String userId, String message, String sessionId, 
                          boolean enableVoiceOutput, VoiceCharacter voiceCharacter) {
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

                log.info("[UserStatusRouter] 收到聊天请求: userId={}, message={}, sessionId={}", 
                        userId, message, finalSessionId);

                // 存储用户对话记录
                userConversationHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
                log.info("[对话记录] userId={}, 当前对话数量={}", userId, userConversationHistory.get(userId).size());

                // 直接从HotelAssistantService获取用户状态
                String userStatus = hotelAssistantService.userStatusMap.getOrDefault(userId, "IN_ROOM");
                log.info("[UserStatusRouter] 用户状态: userId={}, status={}", userId, userStatus);

                // 根据状态执行不同逻辑
                if ("IN_PARK".equals(userStatus)) {
                    handleInParkLogic(userId, message, finalSessionId,
                            voiceCharacter, finalEmitter);
                }else if ("TAKE_HOME".equals(userStatus)) {
                    handleTakeHomeLogic(userId, message, finalSessionId,
                            voiceCharacter, finalEmitter);
                }
                else {
                    // IN_ROOM 或其他状态，默认执行房间内逻辑
                    handleInRoomLogic(userId, message, finalSessionId,
                            voiceCharacter, finalEmitter);
                }

                log.info("[SSE] sessionId: {} 消息处理完成，保持连接", finalSessionId);

            } catch (Exception e) {
                log.error("[SSE] sessionId: {}, 处理消息失败: {}", finalSessionId, e.getMessage(), e);
                sseEventSender.sendError(finalSessionId, finalEmitter, e.getMessage());
            }
        });

        return finalEmitter;
    }

    private void handleTakeHomeLogic(String userId, String message, String sessionId,
                                     VoiceCharacter voiceCharacter, SseEmitter sseEmitter) {
        log.info("[TAKE_HOME] 执行回家路上逻辑: userId={}, message={}", userId, message);
        
        try {
            // 获取用户对话记录
            List<String> conversationHistory = userConversationHistory.getOrDefault(userId, new ArrayList<>());
            log.info("[TAKE_HOME] 用户对话记录数量: {}", conversationHistory.size());
            
            // 将对话记录转换为字符串
            String conversationHistoryText;
            if (conversationHistory.isEmpty()) {
                conversationHistoryText = "用户今天没有对话记录";
            } else {
                conversationHistoryText = String.join("\n", conversationHistory);
            }
            
            // 使用TakeHomeAgent生成回忆总结
            log.info("[TAKE_HOME] 开始生成回忆总结...");
            String memoryText = getTakeHomeAgent().generateMemory(conversationHistoryText);
            log.info("[TAKE_HOME] 回忆总结生成完成: {}", memoryText.substring(0, Math.min(50, memoryText.length())));
            
            // 生成语音并发送
            try {
                log.info("[TAKE_HOME] 开始生成语音...");
                String audioPath = sttService.textToSpeechAndSave(
                        memoryText.replace("*", "").replace("#", ""), voiceCharacter);
                
                sseEventSender.sendEvent(sessionId, sseEmitter,
                        SseEventType.VOICE.getEventName(),
                        Map.of(
                                "audioPath", audioPath,
                                "text", memoryText,
                                "type", "TAKE_HOME",
                                "voiceCharacter", voiceCharacter.name()
                        ));
                log.info("[TAKE_HOME-VOICE] 语音生成成功: {}", audioPath);
            } catch (Exception e) {
                log.error("[TAKE_HOME-VOICE] 语音生成失败: {}", e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("[TAKE_HOME] 处理失败: {}", e.getMessage(), e);
            sseEventSender.sendError(sessionId, sseEmitter, "回忆总结生成失败");
        }
    }

    /**
     * 处理园区内逻辑
     * 参考QueueTimeAgent.inParkChat方法和提示词实现
     */
    private void handleInParkLogic(String userId, String message, String sessionId,
                                   VoiceCharacter voiceCharacter,
                                   SseEmitter sseEmitter) {
        log.info("[IN_PARK] 执行园区内逻辑: userId={}, message={}", userId, message);
        
        try {
            // 使用QueueTimeAgent的inParkChat方法处理用户消息
            String memoryId = "inpark_" + sessionId;
            String agentResponse = getQueueTimeAgent().inParkChat(memoryId, message);
            log.info("[IN_PARK] Agent响应: {}", agentResponse);
            
            // 处理业务逻辑
            if (agentResponse.contains("[DPA_ORDER_SUCCESS]")) {
                // 用户确认下单，生成DPA订单
                log.info("[IN_PARK] 检测到DPA订单成功标记");
                DpaOrder dpaOrder = generateDpaOrder(userId, agentResponse, sessionId);
                if (dpaOrder != null) {
                    // 发送DPA订单结构化数据
                    sseEventSender.sendEvent(sessionId, sseEmitter,
                            SseEventType.STRUCTURED_DATA.getEventName(),
                            Map.of("type", "DPA_ORDER", "data", dpaOrder));
                    log.info("[IN_PARK] 发送DPA订单数据: {}", dpaOrder.getDpaOrderNo());
                }
            } else if (agentResponse.contains("[DPA_ORDER]")) {
                // 推荐尊享卡，提取产品信息
                log.info("[IN_PARK] 检测到DPA产品推荐标记");
                Map<String, Object> dpaProduct = extractDpaFromResponse(agentResponse);
                if (dpaProduct != null && !dpaProduct.isEmpty()) {
                    // 临时存储产品信息，用于后续生成订单
                    tempDpaProductMap.put(sessionId, dpaProduct);
                    
                    // 发送DPA产品结构化数据
                    sseEventSender.sendEvent(sessionId, sseEmitter,
                            SseEventType.STRUCTURED_DATA.getEventName(),
                            Map.of("type", "DPA_PRODUCT", "data", dpaProduct));
                    log.info("[IN_PARK] 发送DPA产品数据: {}", dpaProduct.get("productName"));
                }
            }
            
            // 清理响应中的标记
            String cleanedResponse = cleanInParkTags(agentResponse);

            // 生成语音并发送
            if (!cleanedResponse.isEmpty()) {
                try {
                    String audioPath = sttService.textToSpeechAndSave(
                            cleanedResponse.replace("*", "").replace("#", ""), voiceCharacter);
                    
                    sseEventSender.sendEvent(sessionId, sseEmitter,
                            SseEventType.VOICE.getEventName(),
                            Map.of(
                                    "audioPath", audioPath,
                                    "text", cleanedResponse,
                                    "type", "IN_PARK",
                                    "voiceCharacter", voiceCharacter.name()
                            ));
                    log.info("[IN_PARK-VOICE] 语音生成成功: {}", audioPath);
                } catch (Exception e) {
                    log.error("[IN_PARK-VOICE] 语音生成失败: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("[IN_PARK] 处理失败: {}", e.getMessage(), e);
            sseEventSender.sendError(sessionId, sseEmitter, "园区服务处理失败");
        }
    }

    /**
     * 清理园区内响应中的标记
     */
    private String cleanInParkTags(String response) {
        String cleaned = response;
        // 清理DPA相关标记
        cleaned = cleaned.replaceAll("\\[DPA_ORDER\\][\\s\\S]*?\\[DPA_ORDER\\]", "");
        cleaned = cleaned.replace("[DPA_ORDER_SUCCESS]", "");
        // 清理JSON数据
        cleaned = cleaned.replaceAll("\\{[\\s\\S]*?\"dpaOrderNo\"[\\s\\S]*?\\}", "");
        return cleaned.trim();
    }

    /**
     * 从DPA响应中提取尊享卡产品数据
     * 格式: [DPA_ORDER]{"facilityId":"...","productName":"...","description":"...","price":180,"image":"..."}[DPA_ORDER]
     */
    private Map<String, Object> extractDpaFromResponse(String response) {
        try {
            // 查找 [DPA_ORDER] 标签
            int startTag = response.indexOf("[DPA_ORDER]");
            int endTag = response.lastIndexOf("[DPA_ORDER]");
            
            if (startTag != -1 && endTag != -1 && startTag != endTag) {
                // 提取两个标签之间的内容
                String jsonContent = response.substring(startTag + "[DPA_ORDER]".length(), endTag).trim();
                log.info("[IN_PARK] 提取到DPA JSON: {}", jsonContent);
                
                // 解析JSON
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> dpaData = mapper.readValue(jsonContent, Map.class);
                
                log.info("[IN_PARK] 成功解析DPA产品: {}", dpaData.get("productName"));
                return dpaData;
            } else {
                log.warn("[IN_PARK] 未找到[DPA_ORDER]标签中的JSON数据");
            }
        } catch (Exception e) {
            log.error("[IN_PARK] 提取DPA数据失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 生成DPA订单
     * 从[DPA_ORDER_SUCCESS]标签中解析大模型返回的完整JSON订单数据
     */
    private DpaOrder generateDpaOrder(String userId, String response, String sessionId) {
        try {
            // 从响应中提取完整的订单JSON
            Map<String, Object> orderData = extractDpaOrderFromResponse(response);
            if (orderData == null || orderData.isEmpty()) {
                log.error("[IN_PARK] 无法提取DPA订单数据");
                return null;
            }
            
            // 构建DPA订单
            DpaOrder dpaOrder = DpaOrder.builder()
                    .dpaOrderNo((String) orderData.get("dpaOrderNo"))
                    .userId(userId)
                    .facilityId((String) orderData.get("facilityId"))
                    .productName((String) orderData.get("productName"))
                    .description((String) orderData.get("description"))
                    .price(convertToInteger(orderData.get("price")))
                    .image((String) orderData.get("image"))
                    .createTime(LocalDateTime.now())
                    .status("CONFIRMED")
                    .build();
            
            // 存储订单
            dpaOrderMap.put(dpaOrder.getDpaOrderNo(), dpaOrder);
            
            log.info("[IN_PARK] DPA订单生成成功: orderNo={}, product={}", 
                    dpaOrder.getDpaOrderNo(), dpaOrder.getProductName());
            
            return dpaOrder;
        } catch (Exception e) {
            log.error("[IN_PARK] 生成DPA订单失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从[DPA_ORDER_SUCCESS]响应中提取完整的DPA订单JSON数据
     * 格式: {"dpaOrderNo":"...","facilityId":"...","productName":"...","description":"...","price":180,"image":"..."}
     */
    private Map<String, Object> extractDpaOrderFromResponse(String response) {
        try {
            // 查找包含dpaOrderNo的JSON对象
            int jsonStart = response.indexOf("{");
            if (jsonStart == -1) {
                log.warn("[IN_PARK] 响应中未找到JSON数据");
                return null;
            }
            
            // 找到包含dpaOrderNo字段的JSON对象
            int dpaOrderNoPos = response.indexOf("\"dpaOrderNo\"");
            if (dpaOrderNoPos == -1) {
                log.warn("[IN_PARK] 响应中未找到dpaOrderNo字段");
                return null;
            }
            
            // 从 dpaOrderNo 字段往前查找最近的 {
            int bracketCount = 0;
            int startPos = jsonStart;
            for (int i = dpaOrderNoPos; i >= 0; i--) {
                char c = response.charAt(i);
                if (c == '}') {
                    bracketCount++;
                } else if (c == '{') {
                    if (bracketCount == 0) {
                        startPos = i;
                        break;
                    }
                    bracketCount--;
                }
            }
            
            // 从 startPos 开始查找匹配的 }
            bracketCount = 0;
            int endPos = -1;
            for (int i = startPos; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') {
                    bracketCount++;
                } else if (c == '}') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
            
            if (endPos == -1) {
                log.warn("[IN_PARK] 无法找到完整的JSON对象");
                return null;
            }
            
            String jsonContent = response.substring(startPos, endPos);
            log.info("[IN_PARK] 提取到DPA订单JSON: {}", jsonContent);
            
            // 解析JSON
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> orderData = mapper.readValue(jsonContent, Map.class);
            
            log.info("[IN_PARK] 成功解析DPA订单: orderNo={}, product={}", 
                    orderData.get("dpaOrderNo"), orderData.get("productName"));
            return orderData;
        } catch (Exception e) {
            log.error("[IN_PARK] 提取DPA订单数据失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 转换价格为Integer类型
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("[IN_PARK] 无法转换价格: {}", value);
            return 0;
        }
    }

    /**
     * 处理房间内逻辑
     */
    private void handleInRoomLogic(String userId, String message, String sessionId,
                                   VoiceCharacter voiceCharacter,
                                   SseEmitter sseEmitter) {
        log.info("[IN_ROOM] 执行房间内逻辑: userId={}, message={}", userId, message);
        
        try {
            // 使用RoomWakeUpAgent处理用户消息
            String memoryId = "room_" + sessionId;
            String agentResponse = getRoomWakeUpAgent().chat(memoryId, message);
            log.info("[IN_ROOM] Agent响应: {}", agentResponse);
            
            // 检测是否需要生成叫醒服务
            if (agentResponse.contains("[GENERATE_WAKEUP]")) {
                log.info("[IN_ROOM] 检测到叫醒服务标记");
                
                // 提取[GENERATE_WAKEUP]标签中的JSON数据
                Map<String, Object> wakeupData = extractWakeupDataFromResponse(agentResponse);
                if (wakeupData != null) {
                    String wakeUpTime = (String) wakeupData.get("wakeUpTime");
                    String wakeUpMessage = (String) wakeupData.get("wakeUpMessage");
                    String answer = (String) wakeupData.get("answer");
                    
                    log.info("[IN_ROOM] 解析结果 - wakeUpTime: {}, wakeUpMessage: {}, answer: {}",
                            wakeUpTime, wakeUpMessage, answer);
                    
                    // 1. 发送answer的文本消息
                    if (answer != null && !answer.isEmpty()) {
                        
                        // 生成answer的语音并发送
                        try {
                            String audioPath = sttService.textToSpeechAndSave(
                                    answer.replace("*", "").replace("#", ""), voiceCharacter);
                            
                            sseEventSender.sendEvent(sessionId, sseEmitter,
                                    SseEventType.VOICE.getEventName(),
                                    Map.of(
                                            "audioPath", audioPath,
                                            "text", answer,
                                            "type", "IN_ROOM",
                                            "voiceCharacter", voiceCharacter.name()
                                    ));
                            log.info("[IN_ROOM-VOICE] answer语音生成成功: {}", audioPath);
                        } catch (Exception e) {
                            log.error("[IN_ROOM-VOICE] answer语音生成失败: {}", e.getMessage(), e);
                        }
                    }
                    
                    // 2. 如果有叫醒时间，生成并调度叫醒服务
                    if (wakeUpTime != null && !wakeUpTime.isEmpty()) {
                        log.info("[IN_ROOM] 检测到叫醒服务: {}", wakeUpTime);
                        
                        // 解析时间
                        LocalDateTime wakeUpDateTime = parseWakeUpTime(wakeUpTime);
                        if (wakeUpDateTime != null) {
                            // 生成叫醒语音
                            String wakeUpAudioPath = null;
                            if (wakeUpMessage != null && !wakeUpMessage.isEmpty()) {
                                try {
                                    wakeUpAudioPath = sttService.textToSpeechAndSave(
                                            wakeUpMessage.replace("*", "").replace("#", ""), voiceCharacter);
                                    log.info("[IN_ROOM] 叫醒语音生成成功: {}", wakeUpAudioPath);
                                } catch (Exception e) {
                                    log.error("[IN_ROOM] 叫醒语音生成失败: {}", e.getMessage(), e);
                                }
                            }
                            
                            // 创建叫醒服务对象
                            WakeUpAssistance wakeUpAssistance = WakeUpAssistance.builder()
                                    .wakeUpId("WAKEUP_" + System.currentTimeMillis())
                                    .userId(userId)
                                    .sessionId(sessionId)
                                    .wakeUpTime(wakeUpDateTime)
                                    .createTime(LocalDateTime.now())
                                    .status("PENDING")
                                    .voicePath(wakeUpAudioPath)
                                    .remark("酒店叫醒服务")
                                    .build();
                            
                            // 存储叫醒服务
                            wakeUpAssistanceMap.put(wakeUpAssistance.getWakeUpId(), wakeUpAssistance);
                            log.info("[WAKEUP] 生成叫醒服务: {}, 时间: {}", 
                                    wakeUpAssistance.getWakeUpId(), wakeUpDateTime);
                            
                            // 发送叫醒服务结构化数据
                            sseEventSender.sendEvent(sessionId, sseEmitter,
                                    SseEventType.STRUCTURED_DATA.getEventName(),
                                    Map.of("type", "WAKE_UP", "data", wakeUpAssistance));
                            log.info("[WAKEUP] 发送叫醒服务数据: {}", wakeUpAssistance.getWakeUpId());
                            
                            // 调度定时任务，到时间自动发送SSE事件
                            wakeUpSchedulerService.scheduleWakeUp(wakeUpAssistance, sseEmitter);
                            log.info("[WAKEUP] 叫醒服务已调度，将在 {} 发送SSE事件", wakeUpDateTime);
                        } else {
                            log.error("[IN_ROOM] 无法解析叫醒时间: {}", wakeUpTime);
                        }
                    }
                }
            } else {
                // 普通对话，直接返回响应
                log.info("[IN_ROOM] 普通对话响应");
                
                // 生成语音并发送
                if (!agentResponse.isEmpty()) {
                    try {
                        String audioPath = sttService.textToSpeechAndSave(
                                agentResponse.replace("*", "").replace("#", ""), voiceCharacter);
                        
                        sseEventSender.sendEvent(sessionId, sseEmitter,
                                SseEventType.VOICE.getEventName(),
                                Map.of(
                                        "audioPath", audioPath,
                                        "text", agentResponse,
                                        "type", "IN_ROOM",
                                        "voiceCharacter", voiceCharacter.name()
                                ));
                        log.info("[IN_ROOM-VOICE] 语音生成成功: {}", audioPath);
                    } catch (Exception e) {
                        log.error("[IN_ROOM-VOICE] 语音生成失败: {}", e.getMessage(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("[IN_ROOM] 处理失败: {}", e.getMessage(), e);
            sseEventSender.sendError(sessionId, sseEmitter, "房间服务处理失败");
        }
    }

    /**
     * 从[GENERATE_WAKEUP]响应中提取叫醒数据
     * 格式: [GENERATE_WAKEUP]```json\n{...}\n```[GENERATE_WAKEUP]
     */
    private Map<String, Object> extractWakeupDataFromResponse(String response) {
        try {
            // 查找 [GENERATE_WAKEUP] 标签
            int startTag = response.indexOf("[GENERATE_WAKEUP]");
            int endTag = response.lastIndexOf("[GENERATE_WAKEUP]");
            
            if (startTag != -1 && endTag != -1 && startTag != endTag) {
                // 提取两个标签之间的内容
                String content = response.substring(startTag + "[GENERATE_WAKEUP]".length(), endTag).trim();
                log.info("[IN_ROOM] 提取到标签内容: {}", content);
                
                // 移除可能的 ```json 和 ``` 标记
                content = content.replace("```json", "").replace("```", "").trim();
                
                // 查找JSON对象
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}");
                
                if (jsonStart != -1 && jsonEnd != -1) {
                    String jsonContent = content.substring(jsonStart, jsonEnd + 1);
                    log.info("[IN_ROOM] 提取到JSON: {}", jsonContent);
                    
                    // 解析JSON
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> wakeupData = mapper.readValue(jsonContent, Map.class);
                    
                    log.info("[IN_ROOM] 成功解析叫醒数据: wakeUpTime={}, wakeUpMessage={}", 
                            wakeupData.get("wakeUpTime"), wakeupData.get("wakeUpMessage"));
                    return wakeupData;
                } else {
                    log.warn("[IN_ROOM] 未找到JSON对象");
                }
            } else {
                log.warn("[IN_ROOM] 未找到[GENERATE_WAKEUP]标签");
            }
        } catch (Exception e) {
            log.error("[IN_ROOM] 提取叫醒数据失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解析叫醒时间（HH:mm格式）
     * 使用当天的日期 + 指定的时分
     */
    private LocalDateTime parseWakeUpTime(String timeStr) {
        try {
            // 已经是HH:mm格式
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                
                // 使用当天的日期
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime result = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                
                log.info("[WAKEUP] 解析时间成功: {} -> {} (当天)", timeStr, result);
                return result;
            }
        } catch (Exception e) {
            log.error("[WAKEUP] 解析时间失败: {}", timeStr, e);
        }
        return null;
    }



}
