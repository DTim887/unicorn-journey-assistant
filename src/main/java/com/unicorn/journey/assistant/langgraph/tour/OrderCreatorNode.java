package com.unicorn.journey.assistant.langgraph.tour;

import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class OrderCreatorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            //需要审批才能创建订单
            context.setNeedApproval(Boolean.TRUE);
            context.setCurrentStep(PlannerApp.ORDER_CREATOR_NODE);
            return WorkflowContext.saveContext(context);
        });
    }

}
