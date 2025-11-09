package com.unicorn.journey.assistant.langgragh4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 工作流订单创建代理
 * 用于在工作流中生成订单详情
 */
public interface WorkflowOrderAgent {

    @SystemMessage(fromResource = "prompt/order-creator-prompt.txt")
    String createOrder(
            @V("planId") String planId,
            @V("visitDate") String visitDate,
            @V("visitorCount") Integer visitorCount,
            @UserMessage String userMessage
    );
}

