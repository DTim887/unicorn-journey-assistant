package com.unicorn.journey.assistant.langgragh4j.hotel.util;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 酒店 SSE 事件发送工具类
 */
@Slf4j
public class HotelSseHelper {

    /**
     * 发送 SSE 事件（从 checkpointService 获取 emitter）
     *
     * @param sessionId 会话 ID
     * @param checkpointService checkpoint 服务
     * @param eventType 事件类型
     * @param data 数据内容
     */
    public static void sendSseEvent(String sessionId,
                                    WorkflowCheckpointService checkpointService,
                                    HotelSSEEventType eventType,
                                    Map<String, Object> data) {
        SseEmitter emitter = checkpointService.getEmitter(sessionId);
        sendSseEvent(sessionId, emitter, eventType, data);
    }

    /**
     * 发送 SSE 事件（直接传入 emitter）
     *
     * @param sessionId 会话 ID
     * @param emitter SSE emitter
     * @param eventType 事件类型
     * @param data 数据内容
     */
    public static void sendSseEvent(String sessionId,
                                    SseEmitter emitter,
                                    HotelSSEEventType eventType,
                                    Map<String, Object> data) {
        if (emitter == null) {
            log.warn("[SSE] Emitter 为空，无法发送事件: sessionId={}, eventType={}", sessionId, eventType.getCode());
            return;
        }

        try {
            // 确保 data 中包含 sseEventType
            Map<String, Object> eventData = new HashMap<>(data);
            eventData.put("sseEventType", eventType.getCode());

            String jsonData = JSONUtil.toJsonStr(eventData);
            log.info("[SSE] 发送事件: sessionId={}, eventType={}, dataLength={}", 
                    sessionId, eventType.getCode(), jsonData.length());

            emitter.send(SseEmitter.event()
                    .name(eventType.getCode())
                    .data(jsonData));

            log.info("[SSE] 事件发送成功: eventType={}", eventType.getCode());
        } catch (Exception e) {
            log.error("[SSE] 发送事件失败: sessionId={}, eventType={}, error={}", 
                    sessionId, eventType.getCode(), e.getMessage(), e);
        }
    }

    /**
     * 发送输出块事件（便捷方法）
     *
     * @param sessionId 会话 ID
     * @param checkpointService checkpoint 服务
     * @param content 内容
     * @param nodeName 节点名称
     */
    public static void sendOutputChunk(String sessionId,
                                       WorkflowCheckpointService checkpointService,
                                       String content,
                                       String nodeName) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("nodeName", nodeName);
        sendSseEvent(sessionId, checkpointService, HotelSSEEventType.OUTPUT_CHUNK, data);
    }

    /**
     * 发送输出块事件（带确认类型）
     *
     * @param sessionId 会话 ID
     * @param checkpointService checkpoint 服务
     * @param content 内容
     * @param nodeName 节点名称
     * @param confirmationType 确认类型
     */
    public static void sendOutputChunk(String sessionId,
                                       WorkflowCheckpointService checkpointService,
                                       String content,
                                       String nodeName,
                                       String confirmationType) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("nodeName", nodeName);
        data.put("confirmationType", confirmationType);
        sendSseEvent(sessionId, checkpointService, HotelSSEEventType.OUTPUT_CHUNK, data);
    }

    /**
     * 发送步骤更新事件
     *
     * @param sessionId 会话 ID
     * @param checkpointService checkpoint 服务
     * @param nodeName 节点名称
     * @param message 消息
     */
    public static void sendStepUpdate(String sessionId,
                                      WorkflowCheckpointService checkpointService,
                                      String nodeName,
                                      String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("nodeName", nodeName);
        data.put("message", message);
        sendSseEvent(sessionId, checkpointService, HotelSSEEventType.STEP_UPDATE, data);
    }
}
