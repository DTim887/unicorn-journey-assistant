package com.unicorn.journey.assistant.langgragh4j.demo;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 简单工作流
 */
@Slf4j
public class SimpleGraphApp {
    public static final String GREETER_STR = "greeter";
    public static final String RESPONDER_STR = "responder";
    public static void main(String[] args) throws GraphStateException {
        // 初始化 node
        GreeterNode greeterNode = new GreeterNode();
        ResponderNode responderNode = new ResponderNode();

        // 定义结构图
        var stateGraph = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)
                // 定义节点
                .addNode(GREETER_STR, node_async(greeterNode))
                .addNode(RESPONDER_STR, node_async(responderNode))
                // 定义边
                .addEdge(START, GREETER_STR)
                .addEdge(GREETER_STR, RESPONDER_STR)
                .addEdge(RESPONDER_STR, END);

        // compile graph
        var compiledGraph = stateGraph.compile();

        GraphRepresentation graph = stateGraph.getGraph(GraphRepresentation.Type.MERMAID, "demo");
        log.info(graph.toString());

        // Run the graph
        // The `stream` method returns an AsyncGenerator.
        // For simplicity, we'll collect results. In a real app, you might process them as they arrive.
        // Here, the final state after execution is the item of interest.

        for (var item : compiledGraph.stream( Map.of( SimpleState.MESSAGES_KEY, "Let's, begin!" ) ) ) {
            log.info( "Item: {}", item );
        }
    }
}
