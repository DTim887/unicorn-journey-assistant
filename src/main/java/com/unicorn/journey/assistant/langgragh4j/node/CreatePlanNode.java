package com.unicorn.journey.assistant.langgragh4j.node;


import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CreatePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 创建行程");

            // TODO: 创建行程

            log.info("业务类型:{}", context.getBusinessTypeEnum());

            // 更新状态
            context.setCurrentStep("创建行程");
            context.setPlanId(UUID.randomUUID().toString());
            return WorkflowContext.saveContext(context);
        });
    }
}
