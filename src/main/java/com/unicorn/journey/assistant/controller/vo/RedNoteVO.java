package com.unicorn.journey.assistant.controller.vo;

import lombok.Data;

@Data
public class RedNoteVO {

    private String id;

    private String xsecToken;

    private String title;

    private String[] riskTags;

    private String desc;

    private int riskLevel;

    private String createDateTime;
}
