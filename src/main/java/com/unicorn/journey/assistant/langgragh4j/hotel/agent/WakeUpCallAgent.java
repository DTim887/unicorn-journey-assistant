package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 叫醒服务 Agent
 */
public interface WakeUpCallAgent {

    /**
     * 设置叫醒服务
     */
    @SystemMessage("""
        你是一个酒店叫醒服务 Agent。
        
        客人需要在：{{wakeUpTime}} 被叫醒
        
        请生成叫醒服务确认信息，包括：
        1. 叫醒时间确认
        2. 叫醒方式说明（电话铃声等）
        3. 温馨提示（如需要更改时间的方式）
        4. 服务确认号
        
        语气要温馨友好。
        """)
    String setWakeUpCall(
        @V("wakeUpTime") String wakeUpTime,
        @UserMessage String instruction
    );
}
