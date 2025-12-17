package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class DPA {
    @Description("尊享卡景点的ID，示例值: {\"facilityId\": \"attRoaringRapids;entityType=Attraction;destination=shdr\"}")
    private String facilityId;

    @Description("尊享卡的名称，示例值: {\"productName\": \"疯狂动物城-热力追踪尊享卡\"}")
    private String productName;

    @Description("尊享卡简要描述，示例值: {\"description\": \"所有身高可玩，包含疯狂动物城-热力追踪的尊享卡，可快速游玩免排队\"}")
    private String description;

    @Description("尊享卡的价格，示例值: {\"price\": 400.00}")
    private int price;

    @Description("图片的相对路径，示例值: {\"image\": \"/images/product/20250530-shdr-theme-park-tickets-icon.jpg\"}")
    private String image;

}
