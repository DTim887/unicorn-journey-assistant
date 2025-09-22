package com.unicorn.journey.assistant.controller.vo;

import lombok.Data;

@Data
public class TokenStatesVO {
    /**
     * 输入token数量
     */
    private Long inputTokens;

    /**
     * 输出token数量
     */
    private Long outputTokens;

    /**
     * 总token数量
     */
    private Long totalTokens;

    public static TokenStatesVO of (long inputTokens, long outputTokens, long totalTokens){
        TokenStatesVO tokenStatesVO = new TokenStatesVO();
        tokenStatesVO.setInputTokens(inputTokens);
        tokenStatesVO.setOutputTokens(outputTokens);
        tokenStatesVO.setTotalTokens(totalTokens);
        return tokenStatesVO;
    }
}
