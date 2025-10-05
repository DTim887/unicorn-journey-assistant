package com.unicorn.journey.assistant.langgragh4j.demo;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

@Slf4j
class GreeterNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        log.info("GreeterNode executing. Current messages: {}", state.messages());
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}

@Slf4j
// Node that adds a response
class ResponderNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        log.info("ResponderNode executing. Current messages: {}", state.messages());
        List<String> currentMessages = state.messages();

        // 状态判断
        if (currentMessages.contains("Hello from GreeterNode!")) {
            return Map.of(SimpleState.MESSAGES_KEY, "Acknowledged greeting!");
        }

        return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
    }
}