package com.unicorn.journey.assistant.langgragh4j.service;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流检查点存储服务
 * 用于保存和恢复工作流的中间状态，支持用户确认场景（人在环路）
 */
@Slf4j
@Service
public class WorkflowCheckpointService {

    /**
     * 存储暂停的工作流状态
     * Key: sessionId, Value: MessagesState
     */
    private final Map<String, MessagesState<String>> pausedStates = new ConcurrentHashMap<>();

    /**
     * 存储与客户端的 SSE 连接
     * Key: sessionId, Value: SseEmitter
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 保存暂停的工作流状态
     */
    public void savePausedState(String sessionId, MessagesState<String> state) {
        log.info("保存暂停的工作流状态: sessionId={}", sessionId);
        pausedStates.put(sessionId, state);
    }

    /**
     * 获取暂停的工作流状态
     */
    public MessagesState<String> getPausedState(String sessionId) {
        MessagesState<String> state = pausedStates.get(sessionId);
        if (state != null) {
            log.info("获取暂停的工作流状态: sessionId={}", sessionId);
        } else {
            log.error("未找到暂停的工作流状态: sessionId={}", sessionId);
        }
        return state;
    }

    /**
     * 移除暂停的工作流状态
     */
    public MessagesState<String> removePausedState(String sessionId) {
        log.info("移除暂停的工作流状态: sessionId={}", sessionId);
        return pausedStates.remove(sessionId);
    }

    /**
     * 保存 SSE 连接
     */
    public void saveEmitter(String sessionId, SseEmitter emitter) {
        log.info("保存 SSE 连接: sessionId={}", sessionId);
        emitters.put(sessionId, emitter);
    }

    /**
     * 获取 SSE 连接
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitters.get(sessionId);
    }

    /**
     * 移除 SSE 连接
     */
    public void removeEmitter(String sessionId) {
        log.info("移除 SSE 连接: sessionId={}", sessionId);
        emitters.remove(sessionId);
    }

    /**
     * 清理资源
     */
    public void cleanupSession(String sessionId) {
        pausedStates.remove(sessionId);
        emitters.remove(sessionId);
        log.info("资源已清理: sessionId={}", sessionId);
    }
}

