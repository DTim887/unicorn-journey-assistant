package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Refund {

    @Description("退款的订单ID")
    private String orderId;

    @Description("退款的用户ID")
    private int userId;

    @Description("订单总金额")
    private int totalPrice;

    @Description("退款的金额")
    private int refundPrice;

    @Description("退款的类型: 全额退款，差价退款")
    private String type;

    @Description("退款的时间")
    private String refundTime;
}
