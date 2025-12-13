package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Summary Agent - 总结代理
 * 负责汇总多个 Agent 的输出，提供统一的、连贯的用户体验
 */
public interface SummaryAgent {

    @SystemMessage(fromResource = "prompt/summary-prompt.txt")
    String summarizeResponses(@UserMessage String agentResponses);
}

