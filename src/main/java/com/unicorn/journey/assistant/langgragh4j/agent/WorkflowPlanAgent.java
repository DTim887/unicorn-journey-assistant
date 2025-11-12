package com.unicorn.journey.assistant.langgragh4j.agent;

import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * 工作流行程创建代理
 * 用于在工作流中生成行程规划
 */
public interface WorkflowPlanAgent {

    @SystemMessage(fromResource = "prompt/plan-creator-prompt.txt")
    String createPlan(
            @V("user") User user,
            @V("visitDate") String visitDate,
            @V("visitorCount") Integer visitorCount,
            @MemoryId String memoryId,
            @UserMessage String userMessage
    );

    @SystemMessage(fromResource = "prompt/plan-creator-prompt.txt")
    Flux<String> createPlanStream(
            @V("confirmWorkflowContext") ConfirmWorkflowContext context,
            @UserMessage String userMessage
    );
}

