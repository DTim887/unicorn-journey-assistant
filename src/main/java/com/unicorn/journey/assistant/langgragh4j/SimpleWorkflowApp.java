package com.unicorn.journey.assistant.langgragh4j;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 简单工作流应用
 */
@Slf4j
public class SimpleWorkflowApp {

    static AsyncNodeAction<MessagesState<String>> makeNode(String message) {
        return node_async(state -> {
            log.info("执行节点: {}", message);
            return Map.of("messages", message);
        });
    }

    public static void main(String[] args) throws GraphStateException {
        // 创建工作流图
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 添加节点
                .addNode("recommend_attraction", makeNode("推荐景点"))
                .addNode("create_plan", makeNode("创建行程"))
                .addNode("recommend_product", makeNode("推荐商品"))
                .addNode("generate_order", makeNode("生成订单"))
                .addNode("cancel_order", makeNode("取消订单"))

                // 添加边
                .addEdge(START, "recommend_attraction")                // 开始 -> 推荐景点
                .addEdge("recommend_attraction", "create_plan")    // 推荐景点 -> 创建行程
                .addEdge("create_plan", "recommend_product")             // 创建行程 -> 推荐商品
                .addEdge("recommend_product", "generate_order")              // 推荐商品 -> 生成订单
                .addEdge("generate_order", "cancel_order")     // 生成订单 -> 取消订单
                .addEdge("cancel_order", END)                  // 取消订单 -> 结束

                // 编译工作流
                .compile();

        log.info("开始执行简单工作流");

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图: \n{}", graph.content());

        // 执行工作流
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of())) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            log.info("步骤输出: {}", step);
            stepCounter++;
        }

        log.info("简单工作流执行完成！");
    }
}
