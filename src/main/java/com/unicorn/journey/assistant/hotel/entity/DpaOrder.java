package com.unicorn.journey.assistant.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DPA尊享卡订单实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DpaOrder {

    /**
     * 订单号（buyDpa工具返回值）
     */
    private String dpaOrderNo;
    
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 设施ID
     */
    private String facilityId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品描述
     */
    private String description;
    
    /**
     * 价格
     */
    private Integer price;
    
    /**
     * 产品图片
     */
    private String image;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单状态：PENDING, CONFIRMED, CANCELLED
     */
    private String status;
    
}
