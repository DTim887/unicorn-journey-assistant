package com.unicorn.journey.assistant.langgragh4j.model.response;

import lombok.Data;

@Data
public class ConfirmRequest {
    /**
     * 是否确认 true 确认 false 拒绝
     */
    private boolean confirmed;

    /**
     * 确认类型 PLAN / ORDER
     */
    private String confirmType;
}
