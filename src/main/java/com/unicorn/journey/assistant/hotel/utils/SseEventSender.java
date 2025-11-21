package com.unicorn.journey.assistant.hotel.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE事件发送统一工具类
 */
@Slf4j
@Component
public class SseEventSender {
    
    private final ObjectMapper objectMapper;
    
    public SseEventSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 发送SSE事件
     * 
     * @param sessionId 会话ID
     * @param emitter SSE发射器
     * @param eventType 事件类型
     * @param data 数据
     */
    public void sendEvent(String sessionId, SseEmitter emitter, String eventType, Object data) {
        // 空值检查
        if (emitter == null) {
            log.warn("[SSE] sessionId: {}, emitter为空，无法发送事件: {}", sessionId, eventType);
            return;
        }
        
        try {
            // 构建事件
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name(eventType)
                    .data(data);
            
            // 发送事件
            emitter.send(eventBuilder);
            
            // 记录日志
            String dataPreview = getDataPreview(data);
            log.info("[SSE] sessionId: {}, 发送事件成功 - 事件类型: {}, 数据长度: {}, 数据预览: {}", 
                    sessionId, eventType, getDataLength(data), dataPreview);
            
        } catch (IOException e) {
            log.error("[SSE] sessionId: {}, 发送事件失败 - 事件类型: {}, 错误信息: {}", 
                    sessionId, eventType, e.getMessage(), e);
            completeWithError(emitter, sessionId, e);
        } catch (Exception e) {
            log.error("[SSE] sessionId: {}, 发送事件异常 - 事件类型: {}, 错误信息: {}", 
                    sessionId, eventType, e.getMessage(), e);
            completeWithError(emitter, sessionId, e);
        }
    }
    
    /**
     * 发送菜单数据
     */
    public void sendMenuData(String sessionId, SseEmitter emitter, Object menuData) {
        Map<String, Object> eventData = Map.of(
                "type", "MENU",
                "data", menuData
        );
        sendEvent(sessionId, emitter, "structured_data", eventData);
    }
    
    /**
     * 发送订单数据
     */
    public void sendOrderData(String sessionId, SseEmitter emitter, Object orderData) {
        Map<String, Object> eventData = Map.of(
                "type", "ORDER",
                "data", orderData
        );
        sendEvent(sessionId, emitter, "structured_data", eventData);
    }
    
    /**
     * 发送行程数据
     */
    public void sendPlanData(String sessionId, SseEmitter emitter, Object planData) {
        Map<String, Object> eventData = Map.of(
                "type", "PLAN",
                "data", planData
        );
        sendEvent(sessionId, emitter, "structured_data", eventData);
    }
    
    /**
     * 发送错误消息
     */
    public void sendError(String sessionId, SseEmitter emitter, String errorMessage) {
        Map<String, Object> eventData = Map.of(
                "error", errorMessage,
                "message", "处理失败"
        );
        sendEvent(sessionId, emitter, "error", eventData);
    }
    
    /**
     * 完成SSE连接
     */
    public void complete(SseEmitter emitter, String sessionId) {
        if (emitter == null) {
            log.warn("[SSE] sessionId: {}, emitter为空，无法完成连接", sessionId);
            return;
        }
        
        try {
            emitter.complete();
            log.info("[SSE] sessionId: {}, 连接正常完成", sessionId);
        } catch (Exception e) {
            log.error("[SSE] sessionId: {}, 完成连接异常: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 异常完成SSE连接
     */
    public void completeWithError(SseEmitter emitter, String sessionId, Throwable error) {
        if (emitter == null) {
            log.warn("[SSE] sessionId: {}, emitter为空，无法完成异常连接", sessionId);
            return;
        }
        
        try {
            emitter.completeWithError(error);
            log.error("[SSE] sessionId: {}, 连接异常完成: {}", sessionId, error.getMessage());
        } catch (Exception e) {
            log.error("[SSE] sessionId: {}, 完成异常连接失败: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取数据预览（前300字符）
     */
    private String getDataPreview(Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            if (jsonData.length() > 300) {
                return jsonData.substring(0, 300) + "...";
            }
            return jsonData;
        } catch (Exception e) {
            return "无法序列化数据";
        }
    }
    
    /**
     * 获取数据长度
     */
    private int getDataLength(Object data) {
        try {
            return objectMapper.writeValueAsString(data).length();
        } catch (Exception e) {
            return 0;
        }
    }
}
