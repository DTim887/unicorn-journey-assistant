package com.unicorn.journey.assistant.langgraph.tour;

import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class HelloNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            context.setCurrentStep("HELLO");
            return WorkflowContext.saveContext(context);
        });
    }
}
