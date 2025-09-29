package com.unicorn.journey.assistant.controller.vo;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PlanVO {

    /**
     * plan id
     */
    @Description("行程id")
    private Long id;

    /**
     * plan 名称
     */
    @Description("行程名称")
    private String planName;

    /**
     * plan 日期
     */
    @Description("行程日期")
    private LocalDate planDate;

    /**
     * 景点列表 包含景点id和景点游玩时间段 景点顺序就是PlanAttractionItem在list中的顺序
     */
    @Description("景点列表 包含景点id和景点游玩时间段 景点顺序就是PlanAttractionItem在list中的顺序")
    private List<PlanAttractionItemVO> planAttractionItemVO;

    /**
     * 所属人
     */
    @Description("所属人的用户id")
    private Integer userId;


    @Data
    public static class PlanAttractionItemVO{

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


        /**
         * 景点游玩时间段
         */
        private String visitTimeRange;
    }
}
