package com.unicorn.journey.assistant.langgragh4j.hotel.model;

import lombok.Data;

import java.util.Map;

/**
 * 酒店助手确认请求
 */
@Data
public class HotelConfirmRequest {
    
    /**
     * 操作类型 (approved / rejected / regenerate)
     */
    private String action;
    
    /**
     * 确认类型
     */
    private String confirmType;
    
    /**
     * 附加参数（用于补充缺失的参数）
     */
    private Map<String, Object> params;
    
    /**
     * 用户原始输入（用于记录对话历史）
     */
    private String userInput;
}
