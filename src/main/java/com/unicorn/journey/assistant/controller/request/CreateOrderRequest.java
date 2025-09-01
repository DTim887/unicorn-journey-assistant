package com.unicorn.journey.assistant.controller.request;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @Description("订单所属的用户的ID，示例值: {\"userId\": 2}")
    private int userId;

    @Description("订单中所包含的商品ID，示例值: {\"productId\": 2}")
    private int productId;

    @Description("订单中购买的商品的数量，示例值: {\"quantity\": 3}")
    private int quantity;

    @Description("订单中用户所购买的产品的入园日期，示例值: {\"visitDate\": \"2026-05-04\"}")
    private String visitDate;
}
