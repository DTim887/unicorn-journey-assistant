package com.unicorn.journey.assistant.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单订单实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrder {

    /**
     * 订单 id
     */
    private String orderId;
    
    private String userId;

    /**
     * 具体菜品
     */
    private List<MenuItem> items;

    /**
     * 总花费金额 暂时用double
     */
    private Double totalPrice;
    
    private LocalDateTime createTime;

    /**
     * PENDING, CONFIRMED, CANCELLED
     */
    private String status;
    
}
