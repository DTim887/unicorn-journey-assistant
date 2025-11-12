package com.unicorn.journey.assistant.langgragh4j.agent;

import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * 工作流订单创建代理
 * 用于在工作流中生成订单详情
 */
public interface WorkflowOrderAgent {

    @SystemMessage(fromResource = "prompt/order-creator-prompt.txt")
    String createOrder(
            @V("user") User user,
            @V("planId") String planId,
            @V("visitDate") String visitDate,
            @V("visitorCount") Integer visitorCount,
            @UserMessage String userMessage
    );

    @SystemMessage(fromResource = "prompt/order-creator-prompt.txt")
    Flux<String> createOrderStream(
            @V("confirmWorkflowContext") ConfirmWorkflowContext context,
            @UserMessage String userMessage
    );
}

