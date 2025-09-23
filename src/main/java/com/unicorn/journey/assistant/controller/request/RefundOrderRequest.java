package com.unicorn.journey.assistant.controller.request;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundOrderRequest {

    @Description("退款订单的ID")
    private String orderId;

    @Description("退款的用户ID")
    private int userId;

    @Description("退款的金额")
    private int refundPrice;
}
