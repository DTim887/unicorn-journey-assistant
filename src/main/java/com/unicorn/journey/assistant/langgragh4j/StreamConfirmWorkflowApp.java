package com.unicorn.journey.assistant.langgragh4j;

import com.unicorn.journey.assistant.exception.BusinessException;
import com.unicorn.journey.assistant.exception.ErrorCode;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.enums.ConfirmTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.node.*;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 流式包含确认节点的工作流 - 增强版
 * 使用工作流的自然暂停和恢复机制 + 条件边智能路由 + AI 统筹控制
 * <p>
 * 核心思路：
 * 1. 添加统筹节点，由 AI 控制整体流程
 * 2. 统筹节点检查游玩日期和人数，缺少时暂停等待用户输入
 * 3. 支持行程和订单的"重新生成"功能
 * 4. 创建节点集成 AI 大模型生成个性化内容
 * 5. 使用恢复路由节点 + 条件边，直接跳转到暂停节点
 * <p>
 * 工作流程：
 * START -> resume_router -> orchestrator_check -> create_plan -> confirm_plan 
 * -> [approved: create_order | rejected: orchestrator_summary | regenerate: create_plan]
 * -> confirm_order -> [approved/rejected/regenerate: ...] -> orchestrator_summary -> END
 */
@Slf4j
public class StreamConfirmWorkflowApp {

    private final WorkflowAgentFactory agentFactory;

    public StreamConfirmWorkflowApp(WorkflowAgentFactory agentFactory) {
        this.agentFactory = agentFactory;
    }

    /**
     * 创建带确认功能的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            // 创建统筹节点实例
            OrchestratorNode orchestratorNode = new OrchestratorNode(agentFactory);
            
            return new MessagesStateGraph<String>()
                    // 添加节点 - 创建节点和确认节点完全分开
                    .addNode("resume_router", ResumeRouterNode.create())  // 恢复路由节点
                    .addNode("orchestrator_check", orchestratorNode.createCheckInput())  // 统筹节点-检查输入（AI）
                    .addNode("create_plan", new CreatePlanWithConfirmNode(agentFactory).create())  // 创建行程（AI）
                    .addNode("confirm_plan", ConfirmPlanNode.create())  // 确认行程
                    .addNode("create_order", new CreateOrderWithConfirmNode(agentFactory).create())  // 创建订单（AI）
                    .addNode("confirm_order", ConfirmOrderNode.create())  // 确认订单
                    .addNode("orchestrator_summary", orchestratorNode.createSummary())  // 统筹节点-汇总（AI）
                    .addNode("completion", CompletionNode.create())  // 完成节点（保持兼容）

                    // START 先到恢复路由节点，判断是首次执行还是恢复执行
                    .addEdge(START, "resume_router")
                    
                    // 恢复路由节点的条件边：根据是否为恢复模式，跳转到不同节点
                    .addConditionalEdges("resume_router",
                            edge_async(this::routeOnResume),
                            Map.of(
                                    "orchestrator_check", "orchestrator_check",
                                    "create_plan", "create_plan",
                                    "confirm_plan", "confirm_plan",
                                    "confirm_order", "confirm_order"
                            ))
                    
                    // 统筹节点检查输入后的路由
                    .addConditionalEdges("orchestrator_check",
                            edge_async(this::routeAfterOrchestratorCheck),
                            Map.of(
                                    "create_plan", "create_plan",
                                    END, END  // 等待用户输入时暂停
                            ))
                    
                    // 创建plan后进入确认plan节点
                    .addEdge("create_plan", "confirm_plan")

                    // 行程确认后的条件边（支持：确认/拒绝/重新生成）
                    .addConditionalEdges("confirm_plan",
                            edge_async(this::routeAfterPlanConfirmation),
                            Map.of(
                                    ConfirmTypeEnum.APPROVED.getCode(), "create_order",
                                    ConfirmTypeEnum.REJECTED.getCode(), "orchestrator_summary",
                                    ConfirmTypeEnum.REGENERATE.getCode(), "create_plan",  // 重新生成
                                    END, END  // 等待用户时直接进入 END 节点，暂停
                            ))

                    // 创建订单后进入确认订单节点
                    .addEdge("create_order", "confirm_order")

                    // 订单确认后的条件边（支持：确认/拒绝/重新生成）
                    .addConditionalEdges("confirm_order",
                            edge_async(this::routeAfterOrderConfirmation),
                            Map.of(
                                    ConfirmTypeEnum.APPROVED.getCode(), "orchestrator_summary",
                                    ConfirmTypeEnum.REJECTED.getCode(), "orchestrator_summary",
                                    ConfirmTypeEnum.REGENERATE.getCode(), "create_order",  // 重新生成
                                    END, END  // 等待用户时直接进入 END 节点，暂停
                            ))

                    // 统筹汇总节点到完成节点
                    .addEdge("orchestrator_summary", "completion")
                    
                    // 完成节点到结束
                    .addEdge("completion", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            log.error("工作流创建失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "工作流创建失败: " + e.getMessage());
        }
    }

    /**
     * 恢复路由判断
     * <p>
     * 判断逻辑：
     * 1. 如果是首次执行 (isResuming=false) -> 返回 "orchestrator_check"，从统筹节点开始
     * 2. 如果是恢复执行 (isResuming=true) -> 返回 pausedAtNode 指定的节点名称，直接跳转
     */
    private String routeOnResume(MessagesState<String> state) {
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
        
        log.info("恢复路由判断: isResuming={}, pausedAtNode={}",
            context.isResuming(), context.getPausedAtNode());

        // 如果是恢复case，直接跳转到暂停的节点
        if (context.isResuming() && StringUtils.isNotBlank(context.getPausedAtNode())) {
            String targetNode = context.getPausedAtNode();
            log.info("恢复case：直接跳转到节点 {}", targetNode);
            return targetNode;
        }

        // 首次执行，从统筹节点开始
        log.info("首次执行：从 orchestrator_check 开始");
        return "orchestrator_check";
    }

    /**
     * 统筹节点检查后的路由判断
     * <p>
     * 判断逻辑：
     * 1. 如果需要用户输入（日期或人数缺失）-> 返回 END，暂停等待
     * 2. 如果信息完整 -> 返回 "create_plan"，继续创建行程
     */
    private String routeAfterOrchestratorCheck(MessagesState<String> state) {
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
        
        log.info("统筹路由判断: needConfirmation={}, visitDate={}, visitorCount={}", 
            context.isNeedConfirmation(), context.getVisitDate(), context.getVisitorCount());

        // 如果需要用户输入
        if (context.isNeedConfirmation()) {
            log.info("需要用户输入信息，暂停工作流");
            return END;
        }

        // 信息完整，继续创建行程
        log.info("信息完整，继续创建行程");
        return "create_plan";
    }

    /**
     * 行程确认后的路由判断（支持重新生成）
     * <p>
     * 判断逻辑：
     * 1. 如果需要确认且确认结果为空 -> 返回 END，暂停等待
     * 2. 如果确认结果为 approved -> 继续创建订单
     * 3. 如果确认结果为 rejected -> 跳转到汇总节点
     * 4. 如果确认结果为 regenerate -> 重新生成行程
     */
    private String routeAfterPlanConfirmation(MessagesState<String> state) {
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
        
        log.info("行程确认路由判断: needConfirmation={}, confirmationResult={}", 
            context.isNeedConfirmation(), context.getConfirmationResult());

        // 如果需要确认
        if (context.isNeedConfirmation()) {
            // 如果确认结果为空，暂停工作流
            if (StringUtils.isBlank(context.getConfirmationResult())) {
                log.info("需要用户确认行程，暂停工作流");
                return END;
            }
            
            String result = context.getConfirmationResult();
            
            // 重新生成
            if (ConfirmTypeEnum.REGENERATE.getCode().equalsIgnoreCase(result)) {
                log.info("用户选择重新生成行程");
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                // 清空之前的 planId，让系统重新生成
                context.setPlanId(null);
                return ConfirmTypeEnum.REGENERATE.getCode();
            }
            
            // 确认通过
            if (ConfirmTypeEnum.APPROVED.getCode().equalsIgnoreCase(result)) {
                log.info("用户确认行程，继续创建订单");
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                return ConfirmTypeEnum.APPROVED.getCode();
            }
            
            // 拒绝
            if (ConfirmTypeEnum.REJECTED.getCode().equalsIgnoreCase(result)) {
                log.info("用户拒绝行程，跳转到汇总节点");
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                return ConfirmTypeEnum.REJECTED.getCode();
            }
        }

        // 默认继续
        return ConfirmTypeEnum.APPROVED.getCode();
    }

    /**
     * 订单确认后的路由判断（支持重新生成）
     * <p>
     * 判断逻辑：
     * 1. 如果需要确认且确认结果为空 -> 返回 END，暂停等待
     * 2. 如果确认结果为 approved -> 跳转到汇总节点
     * 3. 如果确认结果为 rejected -> 跳转到汇总节点
     * 4. 如果确认结果为 regenerate -> 重新生成订单
     */
    private String routeAfterOrderConfirmation(MessagesState<String> state) {
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
        
        log.info("订单确认路由判断: needConfirmation={}, confirmationResult={}", 
            context.isNeedConfirmation(), context.getConfirmationResult());

        // 如果需要确认
        if (context.isNeedConfirmation()) {
            // 如果确认结果为空，暂停工作流
            if (StringUtils.isBlank(context.getConfirmationResult())) {
                log.info("需要用户确认订单，暂停工作流");
                return END;
            }
            
            String result = context.getConfirmationResult();
            
            // 重新生成
            if (ConfirmTypeEnum.REGENERATE.getCode().equalsIgnoreCase(result)) {
                log.info("用户选择重新生成订单");
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                // 清空之前的 orderId，让系统重新生成
                context.setOrderId(null);
                return ConfirmTypeEnum.REGENERATE.getCode();
            }
            
            // 确认通过或拒绝，都跳转到汇总节点
            log.info("用户确认/拒绝订单: {}, 跳转到汇总节点", result);
            context.setNeedConfirmation(false);
            context.setConfirmationResult(null);
            return result;
        }

        // 默认继续
        return ConfirmTypeEnum.APPROVED.getCode();
    }
}
