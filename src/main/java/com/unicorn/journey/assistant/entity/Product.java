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

    @Description("产品的ID，示例值: {\"id\": 2}")
    private int id;

    @Description("产品的名称，示例值: {\"productName\": \"上海迪士尼乐园一日票\"}")
    private String productName;

    @Description("产品的简要描述，示例值: {\"description\": \"可在指定的日期畅玩上海迪士尼乐园。\"}")
    private String description;

    @Description("产品的每日价格和库存信息，示例值: {\"inventoryCalendar\": [{\"date\": \"2026-05-01\", \"price\": 475, \"inventory\": 100},{\"date\": \"2026-05-02\", \"price\": 400, \"inventory\": 20}]}")
    private InventoryCalendar[] inventoryCalendar;

    @Description("可游玩的景点ID数组，如果为空则表示没有景点可用。示例值: {\"attractionId\": 2} ")
    private Integer[] attractionId;

    @Description("用户的图片的相对路径，示例值: {\"image\": \"/images/product/20250530-shdr-theme-park-tickets-icon.jpg\"}")
    private String image;

    @Getter
    @Setter
    public static class InventoryCalendar {

        @Description("指定的日期，示例值: {\"date\": \"2026-05-01\"}")
        private String date;

        @Description("指定日期的价格，示例值: {\"price\": 400.00}")
        private Integer price;

        @Description("指定日期的库存，为0则表示当日没有库存，示例值: {\"inventory\": 20}")
        private Integer inventory;
    }
}
