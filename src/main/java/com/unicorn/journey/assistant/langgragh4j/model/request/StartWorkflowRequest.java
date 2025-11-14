package com.unicorn.journey.assistant.langgragh4j.model.request;

import lombok.Data;

/**
 * 启动工作流请求
 */
@Data
public class StartWorkflowRequest {
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户输入的提示词/需求
     */
    private String prompt;
}
