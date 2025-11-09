package com.unicorn.journey.assistant.langgragh4j.model.request;

import lombok.Data;

@Data
public class ConfirmRequest {
    /**
     * 确认类型 PLAN / ORDER
     */
    private String confirmType;

    /**
     * 操作类型: approved / rejected / regenerate
     * regenerate 表示用户选择重新生成
     */
    private String action;

    /**
     * 游玩日期（用户补充信息时提供）
     */
    private String visitDate;

    /**
     * 游玩人数（用户补充信息时提供）
     */
    private Integer visitorCount;
}
