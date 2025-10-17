package com.unicorn.journey.assistant.langgraph.demo1;

import com.unicorn.journey.assistant.chat.AiAgent;
import com.unicorn.journey.assistant.chat.AiAgentFactory;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.langgraph.demo.SimpleState;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

@Slf4j
public class MyFirstNode implements NodeAction<MyAgentState> {

    @Override
    public Map<String, Object> apply(MyAgentState myAgentState) throws Exception {
        log.info("[MyFirstNode] Executing");
        AiAgentFactory aiAgentFactory = SpringBeanUtils.getBean(AiAgentFactory.class);
        AiAgent aiAgent = aiAgentFactory.createAiService(Assistants.DUFFY);
        String response = aiAgent.test("duffy_2", "你好");
        return Map.of(SimpleState.MESSAGES_KEY, response + "<br>");
    }
}
