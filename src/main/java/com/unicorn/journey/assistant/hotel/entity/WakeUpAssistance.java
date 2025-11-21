package com.unicorn.journey.assistant.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 叫醒服务实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WakeUpAssistance {
    
    /**
     * 叫醒服务ID
     */
    private String wakeUpId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 叫醒时间
     */
    private LocalDateTime wakeUpTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 状态：PENDING(待执行)、ACTIVE(已激活)、COMPLETED(已完成)、CANCELLED(已取消)
     */
    private String status;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 叫醒语音文件路径
     */
    private String voicePath;
    
    /**
     * 会话ID（用于发送SSE事件）
     */
    private String sessionId;
}
