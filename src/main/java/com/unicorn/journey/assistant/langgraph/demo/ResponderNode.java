package com.unicorn.journey.assistant.langgraph.demo;

import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

public class ResponderNode implements NodeAction<SimpleState> {

    @Override
    public Map<String, Object> apply(SimpleState simpleState) throws Exception {
        System.out.println("ResponderNode: " + simpleState.messages());
        List<String> currentMessages = simpleState.messages();
        if (currentMessages.contains("Hello from GreeterNode!")) {
            return Map.of(SimpleState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
    }
}
