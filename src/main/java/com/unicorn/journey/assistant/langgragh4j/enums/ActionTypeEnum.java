package com.unicorn.journey.assistant.langgragh4j.enums;

import lombok.Getter;

/**
 * 用户操作类型枚举
 */
@Getter
public enum ActionTypeEnum {
    APPROVED("approved", "确认通过"),
    REJECTED("rejected", "拒绝"),
    REGENERATE("regenerate", "重新生成");

    private final String code;
    private final String description;

    ActionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
