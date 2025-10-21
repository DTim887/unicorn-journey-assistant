package com.unicorn.journey.assistant.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RedNote {

    private String id;

    private String xsecToken;

    private String title;

    private String desc;

    private int riskLevel;

}
