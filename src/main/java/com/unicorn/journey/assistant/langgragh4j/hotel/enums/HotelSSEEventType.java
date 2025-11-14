package com.unicorn.journey.assistant.langgragh4j.hotel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 酒店助手 SSE 事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum HotelSSEEventType {
    
    /**
     * 工作流启动
     */
    WORKFLOW_START("workflow_start"),
    
    /**
     * 步骤更新
     */
    STEP_UPDATE("step_update"),
    
    /**
     * 输出块（流式输出）
     */
    OUTPUT_CHUNK("output_chunk"),
    
    /**
     * 需要确认（菜单确认等）
     */
    CONFIRMATION_REQUIRED("confirmation_required"),
    
    /**
     * 需要输入参数
     */
    INPUT_PARAMS("input_params"),
    
    /**
     * 工作流完成
     */
    WORKFLOW_COMPLETE("workflow_complete"),
    
    /**
     * 工作流错误
     */
    WORKFLOW_ERROR("workflow_error");
    
    private final String code;
}
