package com.unicorn.journey.assistant.entity;


import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RedNote {

    private String id;

    private String xsecToken;

    private String title;

    private String[] riskTags;

    private String desc;

    private int riskLevel;

}
