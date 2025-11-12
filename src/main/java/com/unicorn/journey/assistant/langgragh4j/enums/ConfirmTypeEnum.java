package com.unicorn.journey.assistant.langgragh4j.enums;

import lombok.Getter;

/**
 * 用户确认类型枚举
 */
@Getter
public enum ConfirmTypeEnum {
    // 用户操作类型
    APPROVED("approved", "确认通过"),
    REJECTED("rejected", "拒绝"),
    REGENERATE("regenerate", "重新生成"),
    
    // 确认请求类型
    PLAN("PLAN", "行程确认"),
    ORDER("ORDER", "订单确认");

    private final String code;
    private final String description;

    ConfirmTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
