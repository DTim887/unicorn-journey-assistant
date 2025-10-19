package com.unicorn.journey.assistant.langgragh4j;

import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.checkpoint.CheckpointStorage;
import com.unicorn.journey.assistant.langgragh4j.node.*;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 支持用户确认的工作流应用
 */
@Slf4j
public class ConfirmationWorkflowApp {

    private CompiledGraph<MessagesState<String>> workflow;

    /**
     * 初始化工作流图
     */
    public CompiledGraph<MessagesState<String>> buildWorkflow() {
        try {
            MessagesStateGraph<String> stateGraph = new MessagesStateGraph<>();

            // 添加节点
            stateGraph.addNode("create_plan", CreatePlanWithConfirmNode.create());
            stateGraph.addNode("wait_plan_confirm", WaitForPlanConfirmNode.create());
            stateGraph.addNode("create_order", CreateOrderWithConfirmNode.create());
            stateGraph.addNode("wait_order_confirm", WaitForOrderConfirmNode.create());
            stateGraph.addNode("completion", CompletionNode.create());

            // 添加边
            stateGraph.addEdge(START, "create_plan");
            stateGraph.addEdge("create_plan", "wait_plan_confirm");
            
            // 条件边：等待行程确认
            stateGraph.addConditionalEdges(
                "wait_plan_confirm",
                edge_async(state -> {
                    WorkflowContext context = WorkflowContext.getContext(state);
                    return WaitForPlanConfirmNode.shouldContinue(context);
                }),
                Map.of(
                    "continue", "create_order",
                    "wait", END  // 如果未确认，暂停工作流
                )
            );
            
            stateGraph.addEdge("create_order", "wait_order_confirm");
            
            // 条件边：等待订单确认
            stateGraph.addConditionalEdges(
                "wait_order_confirm",
                edge_async(state -> {
                    WorkflowContext context = WorkflowContext.getContext(state);
                    return WaitForOrderConfirmNode.shouldContinue(context);
                }),
                Map.of(
                    "continue", "completion",
                    "wait", END  // 如果未确认，暂停工作流
                )
            );
            
            stateGraph.addEdge("completion", END);

            this.workflow = stateGraph.compile();
            log.info("工作流构建完成\n");
            GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
            log.info("工作流图:\n{}", graph.content());
            return workflow;

        } catch (Exception e) {
            log.error("构建工作流失败", e);
            throw new RuntimeException("构建工作流失败", e);
        }
    }

    /**
     * 初始化工作流上下文
     */
    public WorkflowContext initializeContext(String userId, String prompt) {
        String sessionId = UUID.randomUUID().toString();
        
        User user = User.builder()
            .id(Integer.parseInt(userId.replaceAll("[^0-9]", "123")))
            .nickname(userId)
            .build();
        
        WorkflowContext context = WorkflowContext.builder()
            .sessionId(sessionId)
            .user(user)
            .originalPrompt(prompt)
            .currentStep("begin")
            .planConfirmed(false)
            .orderConfirmed(false)
            .build();
        
        log.info("初始化工作流上下文: sessionId={}, userId={}", sessionId, userId);
        return context;
    }

    /**
     * 流式执行工作流
     * 返回 Flux 流
     */
    public Flux<String> executeWorkflowWithStream(String userId, String prompt) {
        // 创建一个 Sink 用于发送流式消息
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        Thread.startVirtualThread(() -> {
            try {
                // 构建工作流
                if (workflow == null) {
                    buildWorkflow();
                }
                
                // 初始化上下文
                WorkflowContext context = initializeContext(userId, prompt);
                
                // 发送开始消息
                sink.tryEmitNext("工作流开始执行...\n");
                sink.tryEmitNext("会话ID: " + context.getSessionId() + "\n");
                
                // 构建初始状态
                Map<String, Object> initialState = Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, context);
                
                // 执行工作流
                var resultOpt = workflow.invoke(initialState);
                if (resultOpt.isEmpty()) {
                    throw new RuntimeException("工作流执行失败");
                }
                var result = resultOpt.get();
                
                // 获取最终上下文
                WorkflowContext finalContext = WorkflowContext.getContext(result);
                
                // 根据当前步骤发送相应的消息
                if ("wait_confirm_plan".equals(finalContext.getCurrentStep())) {
                    // 发送行程信息
                    String planSummary = CreatePlanWithConfirmNode.getPlanSummary(finalContext.getPlan());
                    sink.tryEmitNext(planSummary + "\n\n");
                    
                } else if ("wait_confirm_order".equals(finalContext.getCurrentStep())) {
                    // 发送订单信息
                    String orderSummary = CreateOrderWithConfirmNode.getOrderSummary(finalContext.getOrder());
                    sink.tryEmitNext(orderSummary + "\n\n");
                    sink.tryEmitNext("💡 提示: 请调用确认接口来确认订单\n");
                    sink.tryEmitNext("接口: POST /workflow/confirm/order/" + finalContext.getSessionId() + "\n");
                    
                } else if ("final".equals(finalContext.getCurrentStep())) {
                    // 发送完成消息
                    String completionSummary = CompletionNode.getCompletionSummary(finalContext);
                    sink.tryEmitNext(completionSummary + "\n");
                }
                
                sink.tryEmitComplete();
                
            } catch (Exception e) {
                log.error("工作流执行失败", e);
                sink.tryEmitNext("❌ 错误: " + e.getMessage() + "\n");
                sink.tryEmitError(e);
            }
        });
        
        return sink.asFlux();
    }

    /**
     * 恢复工作流（用户确认后继续执行）
     */
    public Flux<String> resumeWorkflow(String sessionId, boolean isConfirmed, String confirmType) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 构建工作流
                if (workflow == null) {
                    buildWorkflow();
                }
                
                // 从 checkpoint 恢复上下文
                CheckpointStorage checkpointStorage = SpringBeanUtils.getBean(CheckpointStorage.class);
                CheckpointStorage.CheckpointData checkpoint = checkpointStorage.getCheckpoint(sessionId);
                
                if (checkpoint == null) {
                    sink.tryEmitNext("错误: 未找到会话信息\n");
                    sink.tryEmitError(new RuntimeException("未找到会话信息: " + sessionId));
                    return;
                }
                
                WorkflowContext context = checkpoint.getContext();
                
                if (!isConfirmed) {
                    sink.tryEmitNext("用户取消" + confirmType + "\n");
                    checkpointStorage.removeCheckpoint(sessionId);
                    sink.tryEmitComplete();
                    return;
                }
                
                // 更新确认状态
                if ("PLAN".equals(confirmType)) {
                    context.setPlanConfirmed(true);
                    sink.tryEmitNext("行程已确认，继续创建订单...\n\n");
                } else if ("ORDER".equals(confirmType)) {
                    context.setOrderConfirmed(true);
                    sink.tryEmitNext("订单已确认，完成工作流...\n\n");
                }
                
                // 构建状态
                Map<String, Object> state = Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, context);
                
                // 继续执行工作流
                var resultOpt = workflow.invoke(state);
                if (resultOpt.isEmpty()) {
                    throw new RuntimeException("工作流执行失败");
                }
                var result = resultOpt.get();
                WorkflowContext finalContext = WorkflowContext.getContext(result);
                
                // 发送相应的消息
                if ("wait_confirm_order".equals(finalContext.getCurrentStep())) {
                    String orderSummary = CreateOrderWithConfirmNode.getOrderSummary(finalContext.getOrder());
                    sink.tryEmitNext(orderSummary + "\n\n");
                    
                } else if ("final".equals(finalContext.getCurrentStep())) {
                    String completionSummary = CompletionNode.getCompletionSummary(finalContext);
                    sink.tryEmitNext(completionSummary + "\n");
                }
                
                sink.tryEmitComplete();
                
            } catch (Exception e) {
                log.error("恢复工作流失败", e);
                sink.tryEmitNext("❌ 错误: " + e.getMessage() + "\n");
                sink.tryEmitError(e);
            }
        });
        
        return sink.asFlux();
    }
}

