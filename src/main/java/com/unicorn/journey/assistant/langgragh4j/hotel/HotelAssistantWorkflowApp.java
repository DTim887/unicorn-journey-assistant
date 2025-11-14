package com.unicorn.journey.assistant.langgragh4j.hotel;

import com.unicorn.journey.assistant.exception.BusinessException;
import com.unicorn.journey.assistant.exception.ErrorCode;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.node.*;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.hotel.tool.RoomTypeQueryTool;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 酒店智能助手工作流应用
 * <p>
 * 工作流程：
 * START -> resume_router -> analyze_tasks -> check_params 
 * -> [MENU_ORDER: menu_order -> confirm_menu | OTHER: task_executor] 
 * -> next_task_router -> summary -> END
 */
@Slf4j
public class HotelAssistantWorkflowApp {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;
    private final RoomTypeQueryTool roomTypeQueryTool;

    public HotelAssistantWorkflowApp(HotelAgentFactory agentFactory,
                                     WorkflowCheckpointService checkpointService,
                                     RoomTypeQueryTool roomTypeQueryTool) {
        this.agentFactory = agentFactory;
        this.checkpointService = checkpointService;
        this.roomTypeQueryTool = roomTypeQueryTool;
    }

    /**
     * 创建酒店助手工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            RouterNode routerNode = new RouterNode(agentFactory, checkpointService, roomTypeQueryTool);
            MenuOrderNode menuOrderNode = new MenuOrderNode(agentFactory, checkpointService);
            ConfirmMenuNode confirmMenuNode = new ConfirmMenuNode(checkpointService);
            TaskExecutorNode taskExecutorNode = new TaskExecutorNode(agentFactory, checkpointService);
            RoomRenewalNode roomRenewalNode = new RoomRenewalNode(agentFactory, checkpointService);
            PaymentNode paymentNode = new PaymentNode(agentFactory, checkpointService);
            SummaryNode summaryNode = new SummaryNode(agentFactory, checkpointService);

            return new MessagesStateGraph<String>()
                    // 添加节点
                    .addNode("resume_router", createResumeRouterNode())  // 恢复路由节点
                    .addNode("analyze_tasks", routerNode.createAnalyzeNode())  // 任务分析节点
                    .addNode("check_params", routerNode.createCheckParamsNode())  // 参数检查节点
                    .addNode("menu_order", menuOrderNode.create())  // 点餐节点
                    .addNode("confirm_menu", confirmMenuNode.create())  // 菜单确认节点
                    .addNode("room_renewal", roomRenewalNode.create())  // 续约订单节点
                    .addNode("payment", paymentNode.create())  // 支付节点
                    .addNode("task_executor", taskExecutorNode.create())  // 其他任务执行节点
                    .addNode("next_task_router", createNextTaskRouterNode())  // 下一个任务路由节点
                    .addNode("summary", summaryNode.create())  // 汇总节点

                    // START 到恢复路由
                    .addEdge(START, "resume_router")

                    // 恢复路由的条件边
                    .addConditionalEdges("resume_router",
                            edge_async(this::routeOnResume),
                            Map.of(
                                    "analyze_tasks", "analyze_tasks",
                                    "check_params", "check_params",
                                    "menu_order", "menu_order",
                                    "confirm_menu", "confirm_menu",
                                    "room_renewal", "room_renewal",
                                    "payment", "payment"
                            ))

                    // 任务分析后进入参数检查
                    .addEdge("analyze_tasks", "check_params")

                    // 参数检查后的条件边
                    .addConditionalEdges("check_params",
                            edge_async(this::routeAfterCheckParams),
                            Map.of(
                                    "menu_order", "menu_order",
                                    "room_renewal", "room_renewal",
                                    "task_executor", "task_executor",
                                    "summary", "summary",
                                    END, END  // 参数缺失时暂停
                            ))

                    // 点餐节点后进入确认节点
                    .addEdge("menu_order", "confirm_menu")

                    // 续约订单节点后进入支付节点（串联）
                    .addEdge("room_renewal", "payment")

                    // 支付节点后进入下一个任务路由
                    .addEdge("payment", "next_task_router")

                    // 菜单确认后的条件边
                    .addConditionalEdges("confirm_menu",
                            edge_async(this::routeAfterConfirmMenu),
                            Map.of(
                                    "menu_order", "menu_order",  // 重新生成
                                    "next_task_router", "next_task_router",  // 确认或拒绝，继续下一个任务
                                    END, END  // 等待用户确认
                            ))

                    // 任务执行节点后进入下一个任务路由
                    .addEdge("task_executor", "next_task_router")

                    // 下一个任务路由的条件边
                    .addConditionalEdges("next_task_router",
                            edge_async(this::routeToNextTask),
                            Map.of(
                                    "check_params", "check_params",  // 还有任务，继续检查参数
                                    "summary", "summary"  // 所有任务完成，进入汇总
                            ))

                    // 汇总节点到结束
                    .addEdge("summary", END)

                    .compile();
        } catch (GraphStateException e) {
            log.error("工作流创建失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "工作流创建失败: " + e.getMessage());
        }
    }

    /**
     * 恢复路由节点
     */
    private AsyncNodeAction<MessagesState<String>> createResumeRouterNode() {
        return AsyncNodeAction.node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("恢复路由节点: isResuming={}", context.isResuming());
            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 下一个任务路由节点
     */
    private AsyncNodeAction<MessagesState<String>> createNextTaskRouterNode() {
        return AsyncNodeAction.node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("下一个任务路由节点");
            
            // 移动到下一个任务
            context.moveToNextTask();
            context.setCurrentStep("准备执行下一个任务");
            
            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 恢复路由判断
     */
    private String routeOnResume(MessagesState<String> state) {
        HotelAssistantContext context = HotelAssistantContext.getContext(state);

        if (context.isResuming() && StringUtils.isNotBlank(context.getPausedAtNode())) {
            log.info("恢复执行，跳转到节点: {}", context.getPausedAtNode());
            return context.getPausedAtNode();
        }

        log.info("首次执行，从任务分析开始");
        return "analyze_tasks";
    }

    /**
     * 参数检查后的路由
     */
    private String routeAfterCheckParams(MessagesState<String> state) {
        HotelAssistantContext context = HotelAssistantContext.getContext(state);

        // 如果需要用户输入参数
        if (context.isNeedConfirmation()) {
            log.info("参数缺失，暂停等待用户输入");
            return END;
        }

        AgentTask currentTask = context.getCurrentTask();
        if (currentTask == null) {
            log.info("没有待执行的任务，进入汇总");
            return "summary";
        }

        // 根据任务类型路由到不同节点
        if ("MENU_ORDER".equals(currentTask.getTaskType())) {
            log.info("路由到点餐节点");
            return "menu_order";
        } else if ("ROOM_RENEWAL".equals(currentTask.getTaskType())) {
            log.info("路由到续约订单节点");
            return "room_renewal";
        } else {
            log.info("路由到任务执行节点");
            return "task_executor";
        }
    }

    /**
     * 菜单确认后的路由
     */
    private String routeAfterConfirmMenu(MessagesState<String> state) {
        HotelAssistantContext context = HotelAssistantContext.getContext(state);

        // 如果需要确认且确认结果为空，暂停等待
        if (context.isNeedConfirmation() && StringUtils.isBlank(context.getConfirmationResult())) {
            log.info("等待用户确认菜单");
            return END;
        }

        String result = context.getConfirmationResult();

        // 重新生成：返回生成菜单节点，生成后会自动再次进入 confirm_menu
        if ("regenerate".equalsIgnoreCase(result)) {
            log.info("用户要求重新生成菜单，返回 menu_order 节点");
            // 清除确认结果，这样重新生成后会再次进入确认流程
            context.setConfirmationResult(null);
            return "menu_order";
        }

        // 确认或拒绝，都继续下一个任务
        log.info("菜单确认完成，继续下一个任务");
        // 清除确认结果，为下次确认做准备
        context.setConfirmationResult(null);
        return "next_task_router";
    }

    /**
     * 路由到下一个任务
     */
    private String routeToNextTask(MessagesState<String> state) {
        HotelAssistantContext context = HotelAssistantContext.getContext(state);

        if (context.hasMoreTasks()) {
            log.info("还有任务待执行，继续");
            return "check_params";
        }

        log.info("所有任务已完成，进入汇总");
        return "summary";
    }
}
