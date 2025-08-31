package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Description("产品id")
    private int id;

    @Description("产品名称")
    private String productName;

    @Description("产品描述")
    private String description;

    @Description("产品不同日期对应的信息")
    private Calendar[] calendar;

    @Description("可以使用此商品的景点id")
    private Integer[] attractionId;

    @Description("产品图片，图片文件路径")
    private String image;

    @Description("所包含的单个产品数量")
    private Integer quantity;

    @Description("产品内容")
    private String content;

    @Data
    private static class Calendar {

        @Description("日期")
        String date;

        @Description("当日价格")
        String price;

        @Description("当日库存")
        Integer inventory;
    }
}
