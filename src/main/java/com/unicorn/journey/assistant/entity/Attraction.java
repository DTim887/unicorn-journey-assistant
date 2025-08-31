package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

/**
 * 游玩设施
 */
@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Attraction {

    /**
     * 景点id
     */
    @Description("景点id")
    private Integer attractionId;

    /**
     * 景点名称
     */
    @Description("景点名称")
    private String attractionName;


    /**
     * 排队时间 单位:分钟
     */
    @Description("排队时间 单位:分钟")
    private Integer queueTime;

    /**
     * 景点照
     */
    @Description("景点照,图片文件路径")
    private String image;

    /**
     * 标签、特征
     */
    @Description("标签、特征")
    private String[] tags;
}
