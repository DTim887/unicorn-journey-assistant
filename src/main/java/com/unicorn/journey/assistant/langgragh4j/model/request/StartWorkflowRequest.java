package com.unicorn.journey.assistant.langgragh4j.model.request;

import lombok.Data;

@Data
public class StartWorkflowRequest {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 游玩日期（可选，格式：YYYY-MM-DD）
     */
    private String visitDate;
    
    /**
     * 游玩人数（可选，1-20人）
     */
    private Integer visitorCount;
}
