package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@ToString
public class Order {
    @Description("订单id")
    private int id;

    @Description("用户id")
    private int userId;

    @Description("商品名称")
    private String productName;

    @Description("商品价格")
    private BigDecimal price;
}
