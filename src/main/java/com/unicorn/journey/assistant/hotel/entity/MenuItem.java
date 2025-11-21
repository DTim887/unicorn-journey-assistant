package com.unicorn.journey.assistant.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 菜品实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {
    
    /**
     * 菜品ID
     */
    private Integer menuId;
    
    /**
     * 菜品名称
     */
    private String name;
    
    /**
     * 菜品类型：中式、西式
     */
    private String category;
    
    /**
     * 口味列表：辣、不辣、酸、甜等
     */
    private List<String> flavors;
    
    /**
     * 菜品图片路径
     */
    private String image;
    
    /**
     * 价格
     */
    private Double price;
    
    /**
     * 菜品描述
     */
    private String description;
}
