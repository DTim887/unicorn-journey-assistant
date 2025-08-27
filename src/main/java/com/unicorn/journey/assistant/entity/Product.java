package com.unicorn.journey.assistant.entity;

import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private int id;
    private String productName;
    private String description;
    private Calendar[] calendar;
    private Integer attractionId;
    private Integer imageId;
    private String content;

    @Data
    private static class Calendar {
        String date;
        String price;
        Integer inventory;
    }
}
