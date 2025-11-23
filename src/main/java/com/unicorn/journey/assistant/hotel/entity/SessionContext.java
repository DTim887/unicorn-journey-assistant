package com.unicorn.journey.assistant.hotel.entity;

import lombok.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话上下文对象
 * 用于存储会话相关的所有数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionContext {

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** SSE 连接对象 */
    private SseEmitter sseEmitter;

    /** 当前会话的菜单列表 */
    private List<MenuItem> selectedMenuItems;

    /** 最后业务数据类型（MENU/CONFIRM_MENU/WAKEUP/ORDER等） */
    private String lastDataType;

    /**
     * 判断是否处于点餐流程
     */
    public boolean isInMenuProcess() {
        return "MENU".equals(lastDataType) || "CONFIRM_MENU".equals(lastDataType);
    }

    /**
     * 判断是否处于叫醒流程
     */
    public boolean isInWakeUpProcess() {
        return "WAKEUP".equals(lastDataType);
    }

    /**
     * 获取业务上下文字符串（用于传递给路由Agent）
     */
    public String getBusinessContextString() {
        if (isInMenuProcess()) {
            return "CURRENT_BUSINESS_TYPE:MENU";
        } else if (isInWakeUpProcess()) {
            return "CURRENT_BUSINESS_TYPE:WAKEUP";
        }
        return "";
    }

    /**
     * 更新最后业务数据类型
     */
    public void updateLastDataType(String dataType) {
        this.lastDataType = dataType;
    }

    /**
     * 清除会话数据
     */
    public void clear() {
        this.selectedMenuItems = null;
        this.lastDataType = "";
        this.sseEmitter = null;
    }
}
