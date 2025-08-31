package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor

//{
//    "id": 1,
//        "productName": "上海迪士尼乐园一日票",
//        "description": "一票畅玩8大主题园区！",
//        "calendar": [
//    {"date": "2026-05-01", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-02", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-03", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-04", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-05", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-06", "price": "¥475", "inventory": 100},
//    {"date": "2026-05-07", "price": "¥475", "inventory": 100}
//    ],
//    "attractionId": null,
//        "image": "/images/product/20250530-shdr-theme-park-tickets-icon.jpg",
//        "quantity": 1,
//        "content": "可当日多次入园，连续两日入园可享约9折优思！"
//},
public class Product {

    @Description("产品的ID，示例值: {\"id\": 2}")
    private int id;

    @Description("产品的名称，示例值: {\"productName\": \"上海迪士尼乐园一日票\"}")
    private String productName;

    @Description("产品的简要描述，示例值: {\"description\": \"可在指定的日期畅玩上海迪士尼乐园。\"}")
    private String description;

    @Description("产品的每日价格和库存信息，示例值: {\"calendar\": [{\"date\": \"2026-05-01\", \"price\": 475, \"inventory\": 100},{\"date\": \"2026-05-02\", \"price\": 400, \"inventory\": 20}]}")
    private Calendar[] calendar;

    @Description("可使用该产品的景点ID，如果为空则表示没有景点可用。示例值: {\"attractionId\": 2} ")
    private Integer[] attractionId;

    @Description("用户的图片的相对路径，示例值: {\"image\": \"/images/product/20250530-shdr-theme-park-tickets-icon.jpg\"}")
    private String image;

    @Description("所包含的产品的数量，如果大于1则表示该产品是一个套装。示例值: {\"quantity\": 1}")
    private Integer quantity;

    @Description("产品的详情描述，示例值: {\"content\": \"可当日多次入园，连续两日入园可享约9折优思！\"}")
    private String content;

    @Data
    private static class Calendar {

        @Description("指定的日期，示例值: {\"date\": \"2026-05-01\"}")
        String date;

        @Description("指定日期的价格，示例值: {\"price\": 400.00}")
        Integer price;

        @Description("指定日期的库存，为0则表示当日没有库存，示例值: {\"inventory\": 20}")
        Integer inventory;
    }
}
