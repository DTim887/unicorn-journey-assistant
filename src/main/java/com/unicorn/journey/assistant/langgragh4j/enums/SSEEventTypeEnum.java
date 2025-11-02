package com.unicorn.journey.assistant.langgragh4j.enums;

import lombok.Getter;

/**
 * SSE 事件消息类型枚举
 */
@Getter
public enum SSEEventTypeEnum {
    WORKFLOW_START("workflow_start", "工作流开始"),
    STEP_UPDATE("step_update", "步骤变更"),
    OUTPUT_CHUNK("output_chunk", "输出流块"),
    CONFIRMATION_REQUIRED("confirmation_required", "待确认请求"),
    WORKFLOW_COMPLETE("workflow_complete", "工作流完成"),
    WORKFLOW_ERROR("workflow_error", "工作流异常");

    public final String code;
    public final String description;


    SSEEventTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
