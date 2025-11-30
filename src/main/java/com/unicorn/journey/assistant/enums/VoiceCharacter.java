package com.unicorn.journey.assistant.enums;

import lombok.Getter;

/**
 * 语音角色枚举
 */
@Getter
public enum VoiceCharacter {
    
    /**
     * 尼克
     */
    NICK("S_bjuHYk4M1"),
    
    /**
     * 朱迪
     */
    JUDY("S_LVaIYk4M1");
    
    /**
     * 语音编码
     */
    private final String code;
    
    VoiceCharacter(String code) {
        this.code = code;
    }
}
