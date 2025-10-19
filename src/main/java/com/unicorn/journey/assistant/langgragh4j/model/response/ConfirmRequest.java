package com.unicorn.journey.assistant.langgragh4j.model.response;

import lombok.Data;

/**
 * 确认请求
 */
@Data
public class ConfirmRequest {
    /**
     * 是否确认
     */
    private boolean confirmed;
    
    /**
     * 确认类型：PLAN（行程）、ORDER（订单）
     */
    private String confirmType;
}
