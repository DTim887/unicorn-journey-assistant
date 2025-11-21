package com.unicorn.journey.assistant.hotel.service;

import com.unicorn.journey.assistant.hotel.entity.WakeUpAssistance;
import com.unicorn.journey.assistant.hotel.enums.SseEventType;
import com.unicorn.journey.assistant.hotel.utils.SseEventSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 叫醒服务定时调度器
 * 负责在叫醒时间到达时发送SSE提醒事件
 */
@Slf4j
@Service
public class WakeUpSchedulerService {
    
    private final SseEventSender sseEventSender;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    
    // 存储已调度的任务，用于取消
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    public WakeUpSchedulerService(SseEventSender sseEventSender) {
        this.sseEventSender = sseEventSender;
    }
    
    /**
     * 调度叫醒服务
     * @param wakeUpService 叫醒服务信息
     * @param emitter SSE连接对象
     */
    public void scheduleWakeUp(WakeUpAssistance wakeUpService, SseEmitter emitter) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime wakeUpTime = wakeUpService.getWakeUpTime();
        
        // 计算延迟时间（秒）
        long delaySeconds = Duration.between(now, wakeUpTime).getSeconds();
        
        if (delaySeconds <= 0) {
            log.warn("[WAKEUP-SCHEDULER] 叫醒时间已过期: {}, 当前时间: {}", wakeUpTime, now);
            return;
        }
        
        log.info("[WAKEUP-SCHEDULER] 调度叫醒服务: wakeUpId={}, 延迟={}秒, 叫醒时间={}", 
                wakeUpService.getWakeUpId(), delaySeconds, wakeUpTime);
        
        // 调度任务
        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            try {
                log.info("[WAKEUP-ALERT] ⏰ 叫醒时间到！wakeUpId={}", wakeUpService.getWakeUpId());
                
                // 发送叫醒提醒事件
                sendWakeUpAlert(wakeUpService, emitter);
                
                // 更新叫醒服务状态为已完成
                wakeUpService.setStatus("COMPLETED");
                
                // 移除已完成的任务
                scheduledTasks.remove(wakeUpService.getWakeUpId());
                
            } catch (Exception e) {
                log.error("[WAKEUP-ALERT] 发送叫醒提醒失败: wakeUpId={}, error={}", 
                        wakeUpService.getWakeUpId(), e.getMessage(), e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
        
        // 存储任务引用
        scheduledTasks.put(wakeUpService.getWakeUpId(), scheduledTask);
        
        log.info("[WAKEUP-SCHEDULER] 叫醒服务已调度: wakeUpId={}, 将在 {} 秒后执行", 
                wakeUpService.getWakeUpId(), delaySeconds);
    }
    
    /**
     * 发送叫醒提醒SSE事件
     */
    private void sendWakeUpAlert(WakeUpAssistance wakeUpService, SseEmitter emitter) {
        String sessionId = wakeUpService.getSessionId();
        String voicePath = wakeUpService.getVoicePath();
        
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("[WAKEUP-ALERT] sessionId为空，无法发送叫醒提醒: wakeUpId={}", 
                    wakeUpService.getWakeUpId());
            return;
        }
        
        if (voicePath == null || voicePath.isEmpty()) {
            log.warn("[WAKEUP-ALERT] 语音文件路径为空，无法播放语音: wakeUpId={}", 
                    wakeUpService.getWakeUpId());
        }
        
        // 构建叫醒提醒数据
        Map<String, Object> alertData = Map.of(
                "wakeUpId", wakeUpService.getWakeUpId(),
                "wakeUpTime", wakeUpService.getWakeUpTime().toString(),
                "voicePath", voicePath != null ? voicePath : "",
                "message", "您的叫醒时间到了！"
        );
        
        // 发送SSE事件
        sseEventSender.sendEvent(
                sessionId, 
                emitter, 
                SseEventType.WAKEUP_ALERT.getEventName(), 
                alertData
        );
        
        log.info("[WAKEUP-ALERT] ✅ 叫醒提醒已发送: wakeUpId={}, sessionId={}, voicePath={}", 
                wakeUpService.getWakeUpId(), sessionId, voicePath);
    }
    
    /**
     * 取消叫醒服务
     * @param wakeUpId 叫醒服务ID
     * @return 是否取消成功
     */
    public boolean cancelWakeUp(String wakeUpId) {
        ScheduledFuture<?> task = scheduledTasks.remove(wakeUpId);
        
        if (task != null) {
            boolean cancelled = task.cancel(false);
            log.info("[WAKEUP-SCHEDULER] 取消叫醒服务: wakeUpId={}, 取消结果={}", wakeUpId, cancelled);
            return cancelled;
        }
        
        log.warn("[WAKEUP-SCHEDULER] 未找到叫醒任务: wakeUpId={}", wakeUpId);
        return false;
    }
    
    /**
     * 获取待执行的叫醒任务数量
     */
    public int getPendingTaskCount() {
        return scheduledTasks.size();
    }
}
