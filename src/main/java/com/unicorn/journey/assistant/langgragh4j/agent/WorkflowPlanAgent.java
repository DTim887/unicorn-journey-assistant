package com.unicorn.journey.assistant.langgragh4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 工作流行程创建代理
 * 用于在工作流中生成行程规划
 */
public interface WorkflowPlanAgent {

    @SystemMessage(fromResource = "prompt/plan-creator-prompt.txt")
    String createPlan(
            @V("visitDate") String visitDate,
            @V("visitorCount") Integer visitorCount,
            @UserMessage String userMessage
    );
}

