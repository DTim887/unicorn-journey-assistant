package com.unicorn.journey.assistant.langgragh4j.ai;

import com.unicorn.journey.assistant.langgragh4j.enums.BusinessTypeEnum;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 智能路由 服务
 */
public interface RouterService {

    /**
     * 根据用户提示词分析业务类型
     */
    @SystemMessage(fromResource = "router_prompt.txt")
    BusinessTypeEnum routeBusinessType(@UserMessage String userPrompt);
}