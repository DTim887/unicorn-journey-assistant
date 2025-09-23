package com.unicorn.journey.assistant.constant;

import lombok.Getter;

@Getter
public enum RefundType {
    ALL("全额退款"),
    PARTIAL("部分退款");

    RefundType(String status) {
    }
}
