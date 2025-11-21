package com.unicorn.journey.assistant.hotel.enums;

/**
 * SSE事件类型枚举
 * 酒店助手系统中使用的所有SSE事件类型
 */
public enum SseEventType {

    /**
     * 首次对话session
     * 首次对话时返回sessionId
     */
    SESSION_CREATED("session_created"),
    
    /**
     * 普通消息事件
     * 用于同步返回AI对话文本内容
     */
    MESSAGE("message"),
    
    /**
     * 结构化数据事件
     * 用于异步返回菜单、订单、行程等结构化数据
     */
    STRUCTURED_DATA("structured_data"),
    
    /**
     * 确认菜单事件
     * 用于发送需要用户确认的菜单数据
     */
    CONFIRM_MENU("confirm_menu"),
    
    /**
     * 语音事件
     * 用于发送语音相关的数据信息
     */
    VOICE("voice"),
    
    /**
     * 叫醒提醒事件
     * 用于在叫醒时间到达时发送提醒
     */
    WAKEUP_ALERT("wakeup_alert"),
    
    /**
     * 错误事件
     * 用于发送错误信息
     */
    ERROR("error");
    
    private final String eventName;
    
    SseEventType(String eventName) {
        this.eventName = eventName;
    }
    
    /**
     * 获取事件名称
     */
    public String getEventName() {
        return eventName;
    }
    
    /**
     * 根据事件名称获取枚举
     * @param eventName 事件名称
     * @return 对应的枚举，如果不存在则返回null
     */
    public static SseEventType fromEventName(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return null;
        }
        
        for (SseEventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return this.eventName;
    }
}
