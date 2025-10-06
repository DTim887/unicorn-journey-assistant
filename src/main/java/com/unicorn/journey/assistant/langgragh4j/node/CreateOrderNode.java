package com.unicorn.journey.assistant.langgragh4j.node;


import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CreateOrderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 创建订单");

            // TODO: 创建订单

            log.info("业务类型:{}", context.getBusinessTypeEnum());
            log.info("行程ID:{}", context.getPlanId());

            // 更新状态
            context.setCurrentStep("创建订单");
            context.setOrderId(UUID.fastUUID().toString());
            return WorkflowContext.saveContext(context);
        });
    }
}
