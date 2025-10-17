package com.unicorn.journey.assistant;

import com.unicorn.journey.assistant.langgraph.demo.GreeterNode;
import com.unicorn.journey.assistant.langgraph.demo.ResponderNode;
import com.unicorn.journey.assistant.langgraph.demo.SimpleState;
import com.unicorn.journey.assistant.langgraph.demo.TestTool;
import com.unicorn.journey.assistant.service.UserOrderService;
import com.unicorn.journey.assistant.service.UserService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@SpringBootTest
class UnicornJourneyAssistantApplicationTests {


    @Resource
    private UserService userService;

    @Resource
    private UserOrderService userOrderService;

    @Resource
    private ChatModel deepseekChatModel;

    @Test
    void contextLoads() {
        try {
            runSimpleGraphApp();
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }
    }


    private void runSimpleGraphApp () throws GraphStateException {
        // Initialize nodes
        GreeterNode greeterNode = new GreeterNode();
        ResponderNode responderNode = new ResponderNode();

        // Define the graph structure
        var stateGraph = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)
                .addNode("greeter", node_async(greeterNode))
                .addNode("responder", node_async(responderNode))
                // Define edges
                .addEdge(StateGraph.START, "greeter") // Start with the greeter node
                .addEdge("greeter", "responder")
                .addEdge("responder", StateGraph.END)   // End after the responder node
                ;

        //  Compile the graph
        // Run the graph
        // The `stream` method returns an AsyncGenerator.
        // For simplicity, we'll collect results. In a real app, you might process them as they arrive.
        // Here, the final state after execution is the item of interest.
//        var compiledGraph = stateGraph.compile();
//
//        for (var item : compiledGraph.stream( Map.of( SimpleState.MESSAGES_KEY, "你是谁"))) {
//            System.out.println( item );
//        }


        //Agent
        var agent = AgentExecutor.builder()
                .chatModel(deepseekChatModel)
                .toolsFromObject(new TestTool())
                .build()
                .compile();

        for (NodeOutput<AgentExecutor.State> item : agent.stream( Map.of( SimpleState.MESSAGES_KEY, Collections.singletonList(UserMessage.from("perform test twice and return number of current active threads"))))) {
            System.out.println( item );
        }
    }

}
