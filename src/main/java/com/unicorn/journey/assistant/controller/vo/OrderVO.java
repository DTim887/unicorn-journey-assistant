package com.unicorn.journey.assistant.controller.vo;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderVO {

    @Description("订单ID，是一个随机生成的UUID")
    private String id;

    @Description("订单所属的用户的ID，示例值: {\"userId\": 2}")
    private int userId;

    @Description("订单中所包含的商品名称，示例值: {\"productName\": \"乐园一日票\"}")
    private String productName;

    @Description("订单中购买的商品的数量，示例值: {\"quantity\": 3}")
    private int quantity;

    @Description("订单的总价格")
    private int totalPrice;

    @Description("订单中用户所购买的产品的入园日期，示例值: {\"visitDate\": \"2026-05-04\"}")
    private String visitDate;
}
