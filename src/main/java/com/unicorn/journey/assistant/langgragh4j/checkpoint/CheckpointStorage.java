package com.unicorn.journey.assistant.langgragh4j.checkpoint;

import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checkpoint 存储
 * 用于保存和恢复工作流状态，支持用户确认后继续执行
 */
@Component
@Slf4j
public class CheckpointStorage {

    /**
     * 存储所有的 checkpoint
     * Key: sessionId, Value: CheckpointData
     */
    private final Map<String, CheckpointData> checkpoints = new ConcurrentHashMap<>();

    /**
     * 保存 checkpoint
     */
    public void saveCheckpoint(String sessionId, WorkflowContext context, String currentNode) {
        CheckpointData data = new CheckpointData();
        data.setContext(context);
        data.setCurrentNode(currentNode);
        data.setTimestamp(LocalDateTime.now());
        
        checkpoints.put(sessionId, data);
        log.info("保存 checkpoint: sessionId={}, node={}", sessionId, currentNode);
    }

    /**
     * 获取 checkpoint
     */
    public CheckpointData getCheckpoint(String sessionId) {
        CheckpointData data = checkpoints.get(sessionId);
        if (data != null) {
            log.info("获取 checkpoint: sessionId={}, node={}", sessionId, data.getCurrentNode());
        }
        return data;
    }

    /**
     * 删除 checkpoint
     */
    public void removeCheckpoint(String sessionId) {
        checkpoints.remove(sessionId);
        log.info("删除 checkpoint: sessionId={}", sessionId);
    }

    /**
     * 检查是否存在 checkpoint
     */
    public boolean hasCheckpoint(String sessionId) {
        return checkpoints.containsKey(sessionId);
    }

    /**
     * 清理过期的 checkpoint（可选，防止内存泄漏）
     * 可以通过定时任务调用
     */
    public void cleanExpiredCheckpoints(int expireHours) {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(expireHours);
        checkpoints.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(expireTime)
        );
        log.info("清理过期 checkpoint，过期时间: {} 小时前", expireHours);
    }

    /**
     * Checkpoint 数据
     */
    @Data
    public static class CheckpointData {
        private WorkflowContext context;
        private String currentNode;
        private LocalDateTime timestamp;
    }
}

