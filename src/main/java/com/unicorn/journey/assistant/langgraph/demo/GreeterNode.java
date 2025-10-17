package com.unicorn.journey.assistant.langgraph.demo;

import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

public class GreeterNode implements NodeAction<SimpleState>{

    @Override
    public Map<String, Object> apply(SimpleState simpleState) throws Exception {
        System.out.println("GreeterNode : " + simpleState.messages());
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}
