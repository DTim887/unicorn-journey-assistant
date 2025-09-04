package com.unicorn.journey.assistant.controller.request;

import com.unicorn.journey.assistant.entity.Order;
import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @Description("订单所属的用户的ID，示例值: {\"userId\": 2}")
    private int userId;

    @Description("订单中包含的要购买产品列表")
    private List<Order.PurchasedProduct> purchasedProducts;

    @Description("订单中用户所购买的产品的入园日期，示例值: {\"visitDate\": \"2026-05-04\"}")
    private String visitDate;

}
