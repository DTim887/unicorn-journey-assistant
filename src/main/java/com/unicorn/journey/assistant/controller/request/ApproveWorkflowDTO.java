package com.unicorn.journey.assistant.controller.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveWorkflowDTO {

    private String workflowId;

    private String approvalResult;
}
