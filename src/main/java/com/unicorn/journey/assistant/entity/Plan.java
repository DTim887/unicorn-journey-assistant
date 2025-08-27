package com.unicorn.journey.assistant.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private Integer planId;

    /**
     * plan 名称
     */
    private String planName;

    /**
     * plan 日期
     */
    private LocalDate planDate;

    /**
     * 景点列表 景点id 景点顺序就是景点id在list中的顺序
     */
    private List<Integer> attractionIds;

    /**
     * 所属人
     */
    private Integer userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
