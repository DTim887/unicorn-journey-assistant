package com.unicorn.journey.assistant.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 行程
 */
@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Plan {
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
    private List<PlanAttractionItem> attractionIds;

    /**
     * 所属人
     */
    @Description("所属人的用户id")
    private Integer userId;


    @Data
    static class PlanAttractionItem{
        /**
         * 景点id
         */
        private Integer attractionId;

        /**
         * 景点游玩时间段
         */
        private String visitTimeRange;
    }

}
