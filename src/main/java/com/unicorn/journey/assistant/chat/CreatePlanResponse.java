package com.unicorn.journey.assistant.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlanResponse {

    private String answer;

    private String toolName;

    private String toolDesc;

    private Boolean needApproval;
}
