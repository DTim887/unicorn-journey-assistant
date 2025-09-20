package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Description("订单ID，是一个随机生成的UUID")
    private String id;

    @Description("订单所属的用户的ID，示例值: {\"userId\": 2}")
    private int userId;

    @Description("订单中包含的要购买产品列表")
    private List<PurchasedProduct> purchasedProducts;

    @Description("订单中用户所购买的产品的入园日期，示例值: {\"visitDate\": \"2026-05-04\"}")
    private String visitDate;

    @Description("订单状态，示例: 待付款，已支付，已退款")
    private String status;

    @Description("订单的总价格")
    private int totalPrice;

    @Getter
    @Setter
    public static class PurchasedProduct {
        @Description("订单中所包含的商品ID，示例值: {\"productId\": 2}")
        private int productId;

        @Description("订单中购买的商品的数量，示例值: {\"quantity\": 3}")
        private int quantity;
    }

}
