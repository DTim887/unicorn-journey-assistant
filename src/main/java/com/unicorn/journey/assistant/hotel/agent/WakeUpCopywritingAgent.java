package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 陪伴服务文案生成Agent
 * 负责生成迷你迪士尼小故事作为叫醒文案
 */
public interface WakeUpCopywritingAgent {
    
    @SystemMessage("你将会在{{wakeUpTime}}叫醒用户。生成一段具有迪士尼风格的唤醒文案，20个字以内。")
    @UserMessage("帮我生成一段叫醒文案")
    String generateWakeUpCopy(
            @V("wakeUpTime") String wakeUpTime
    );
}
