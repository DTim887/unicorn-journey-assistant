package com.unicorn.journey.assistant.langgraph.demo1;

import com.unicorn.journey.assistant.langgraph.demo.SimpleState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

@Slf4j
public class PlanNode2 implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState simpleState) throws Exception {
        log.info("[Plan2] Executing");
        return Map.of();
    }
}
