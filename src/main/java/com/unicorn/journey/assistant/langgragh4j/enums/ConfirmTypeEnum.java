package com.unicorn.journey.assistant.langgragh4j.enums;

import lombok.Getter;

/**
 * 用户确认类型枚举
 */
@Getter
public enum ConfirmTypeEnum {
    APPROVED("approved"),
    REJECTED("rejected");

    private final String code;


    ConfirmTypeEnum(String code) {
        this.code = code;
    }
}
