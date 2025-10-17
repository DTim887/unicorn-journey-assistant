package com.unicorn.journey.assistant.langgraph.demo;

import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class SimpleGraphApp {

    public static void main(String[] args) throws GraphStateException {
        // Initialize nodes
        GreeterNode greeterNode = new GreeterNode();
        GreeterNode2 greeterNode2 = new GreeterNode2();
        GreeterNode3 greeterNode3 = new GreeterNode3();
        ResponderNode responderNode = new ResponderNode();

        // Define the graph structure
        var stateGraph = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)
                .addNode("greeter3", node_async(greeterNode3))
                .addNode("greeter2", node_async(greeterNode2))
                .addNode("greeter", node_async(greeterNode))
                .addNode("responder", node_async(responderNode))
                // Define edges
                .addEdge(StateGraph.START, "greeter2") // Start with the greeter node
                .addEdge("greeter2", "greeter3")
                .addEdge("greeter3", "greeter")
                .addEdge("greeter", "responder")
                .addEdge("responder", StateGraph.END)   // End after the responder node
                ;

        // Compile the graph
        var compiledGraph = stateGraph.compile();

        GraphRepresentation graph = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID);
//        System.out.println("工作流图:\n{}" + graph.content());

        // Run the graph
        // The `stream` method returns an AsyncGenerator.
        // For simplicity, we'll collect results. In a real app, you might process them as they arrive.
        // Here, the final state after execution is the item of interest.
        for (var item : compiledGraph.stream(Map.of(SimpleState.MESSAGES_KEY, "Let's, begin!"))) {
            if (!(item.isEND() || item.isSTART())) {
                System.out.println("APP:" + item.state().messages().getLast());
            }
        }

    }
}
