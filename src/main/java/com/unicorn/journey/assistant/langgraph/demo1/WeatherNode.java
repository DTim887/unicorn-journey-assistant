package com.unicorn.journey.assistant.langgraph.demo1;

import com.unicorn.journey.assistant.chat.AiAgent;
import com.unicorn.journey.assistant.chat.AiAgentFactory;
import com.unicorn.journey.assistant.chat.CreatePlanResponse;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.langgraph.demo.SimpleState;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

@Slf4j
public class WeatherNode implements NodeAction<SimpleState> {

    @Override
    public Map<String, Object> apply(SimpleState simpleState) throws Exception {
        log.info("[Weather] Executing...");
        String message = simpleState.messages().getLast();
        AiAgentFactory aiAgentFactory = SpringBeanUtils.getBean(AiAgentFactory.class);
        AiAgent aiAgent = aiAgentFactory.createAiService(Assistants.JUDY);
        CreatePlanResponse response = aiAgent.weather("judy_1", message);
        return Map.of(SimpleState.MESSAGES_KEY, response.getAnswer() + "<br>");
    }
}
