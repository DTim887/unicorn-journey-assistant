package com.unicorn.journey.assistant.hotel.service;

import com.unicorn.journey.assistant.enums.VoiceCharacter;
import com.unicorn.journey.assistant.hotel.entity.SessionContext;
import com.unicorn.journey.assistant.hotel.enums.SseEventType;
import com.unicorn.journey.assistant.hotel.utils.SseEventSender;
import com.unicorn.journey.assistant.service.STTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
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

    // 存储会话上下文
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    public UserStatusRouterService(HotelAssistantService hotelAssistantService,
                                   SseEventSender sseEventSender,
                                   STTService sttService) {
        this.hotelAssistantService = hotelAssistantService;
        this.sseEventSender = sseEventSender;
        this.sttService = sttService;
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
                    handleInRoomLogic(userId, message, finalSessionId, enableVoiceOutput, 
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

    private void handleTakeHomeLogic(String userId, String message, String finalSessionId, VoiceCharacter voiceCharacter, SseEmitter finalEmitter) {
        // TODO
    }

    /**
     * 处理园区内逻辑
     */
    private void handleInParkLogic(String userId, String message, String sessionId,
                                   VoiceCharacter voiceCharacter,
                                   SseEmitter sseEmitter) {
        log.info("[IN_PARK] 执行园区内逻辑: userId={}, message={}", userId, message);
        
        try {
            // TODO: 实现园区内的业务逻辑
            String response = "";
            
            if (!response.isEmpty()) {
                try {
                    String audioPath = sttService.textToSpeechAndSave(
                            response.replace("*", "").replace("#", ""), voiceCharacter);
                    
                    sseEventSender.sendEvent(sessionId, sseEmitter,
                            SseEventType.VOICE.getEventName(),
                            // TODO 需要定义数据类型
                            Map.of(
                                    "audioPath", audioPath,
                                    "text", response,
                                    "type", "",
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
     * 处理房间内逻辑
     */
    private void handleInRoomLogic(String userId, String message, String sessionId,
                                   boolean enableVoiceOutput, VoiceCharacter voiceCharacter,
                                   SseEmitter sseEmitter) {
        log.info("[IN_ROOM] 执行房间内逻辑: userId={}, message={}", userId, message);
        
        try {
            // TODO: 实现房间内的业务逻辑

            String response = "";
            
            // 如果启用语音输出，生成语音
            if (!response.isEmpty()) {
                try {
                    String audioPath = sttService.textToSpeechAndSave(
                            response.replace("*", "").replace("#", ""), voiceCharacter);
                    
                    sseEventSender.sendEvent(sessionId, sseEmitter,
                            SseEventType.VOICE.getEventName(),
                            // TODO 需要定义数据类型
                            Map.of(
                                    "audioPath", audioPath,
                                    "text", response,
                                    "type", "assistant",
                                    "voiceCharacter", voiceCharacter.name()
                            ));
                    log.info("[IN_ROOM-VOICE] 语音生成成功: {}", audioPath);
                } catch (Exception e) {
                    log.error("[IN_ROOM-VOICE] 语音生成失败: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("[IN_ROOM] 处理失败: {}", e.getMessage(), e);
            sseEventSender.sendError(sessionId, sseEmitter, "房间服务处理失败");
        }
    }

}
