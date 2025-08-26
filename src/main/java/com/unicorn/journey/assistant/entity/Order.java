package com.unicorn.journey.assistant.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@ToString
public class Order {
    private int id;
    private int userId;
    private String productName;
    private BigDecimal price;
}
