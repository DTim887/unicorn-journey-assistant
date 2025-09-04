package com.unicorn.journey.assistant.constant;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("待支付"),
    SUCCESS("已付款"),
    REFUND("已退款");


    OrderStatus(String status) {
    }
}
