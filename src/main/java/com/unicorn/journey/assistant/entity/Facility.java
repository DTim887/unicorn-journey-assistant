package com.unicorn.journey.assistant.entity;

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
public class Facility {
    private int id;

    /**
     * 景点名称
     */
    private String facilityName;

    /**
     * 景点照
     */
    private String facilityPhoto;

    /**
     * 标签
     */
    private String[] tags;
}
