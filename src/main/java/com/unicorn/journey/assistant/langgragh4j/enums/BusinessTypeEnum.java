package com.unicorn.journey.assistant.langgragh4j.enums;

import lombok.Getter;

/**
 * 业务类型枚举
 */
@Getter
public enum BusinessTypeEnum {
    CREATE_PLAN("judy", "推荐景点并创建行程"),
    CREATE_ORDER("duffy", "推荐商品并生成订单"),
    REFUND("wennie","订单退款");

    private final String assistant;
    private final String desc;

    BusinessTypeEnum(String assistant, String desc) {
        this.assistant = assistant;
        this.desc = desc;
    }

}
