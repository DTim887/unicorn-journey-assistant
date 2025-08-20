package com.unicorn.journey.assistant.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class Order {
    private int id;
    private String orderNo;
}
