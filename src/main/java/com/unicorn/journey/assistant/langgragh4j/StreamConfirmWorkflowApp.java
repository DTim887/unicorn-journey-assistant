package com.unicorn.journey.assistant.langgragh4j;

import com.unicorn.journey.assistant.exception.BusinessException;
import com.unicorn.journey.assistant.exception.ErrorCode;
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
 * 流式包含确认节点的工作流
 * 使用工作流的自然暂停和恢复机制 + 条件边智能路由
 * <p>
 * 核心思路：
 * 1. 创建节点和确认节点分开，确认节点负责暂停工作流
 * 2. 在确认节点中检查确认状态，如果未确认则设置标志并记录暂停位置
 * 3. 在条件边中判断，如果需要确认且用户未确认，返回 END 让工作流自然结束
 * 4. 保存当前 MessagesState 到暂停状态
 * 5. 用户确认后，更新状态并重新调用 graph.stream() 继续执行
 * 6. 使用恢复路由节点 + 条件边，直接跳转到暂停节点，避免从头执行
 */
@Slf4j
public class StreamConfirmWorkflowApp {

    /**
     * 创建带确认功能的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 创建节点和确认节点完全分开
                    .addNode("resume_router", ResumeRouterNode.create())  // 恢复路由节点
                    .addNode("create_plan", CreatePlanWithConfirmNode.create())
                    .addNode("confirm_plan", ConfirmPlanNode.create())
                    .addNode("create_order", CreateOrderWithConfirmNode.create())
                    .addNode("confirm_order", ConfirmOrderNode.create())
                    .addNode("completion", CompletionNode.create()) // 只有经过 completion 节点 才算业务执行成功

                    // START 先到恢复路由节点，判断是首次执行还是恢复执行
                    .addEdge(START, "resume_router")
                    
                    // 恢复路由节点的条件边：根据是否为恢复模式，跳转到不同节点
                    .addConditionalEdges("resume_router",
                            edge_async(this::routeOnResume),
                            Map.of(
                                    "create_plan", "create_plan",
                                    "confirm_plan", "confirm_plan",
                                    "confirm_order", "confirm_order"
                            ))
                    
                    // 创建plan后进入确认plan节点
                    .addEdge("create_plan", "confirm_plan")

                    // 行程创建后的条件边（判断是否需要暂停等待用户确认）
                    .addConditionalEdges("confirm_plan",
                            edge_async(this::routeAfterConfirmation),
                            Map.of(
                                    ConfirmTypeEnum.APPROVED.getCode(), "create_order",
                                    ConfirmTypeEnum.REJECTED.getCode(), "completion",
                                    END, END  // 等待用户时直接进入 END 节点，暂停
                            ))

                    // 创建订单后进入确认订单节点
                    .addEdge("create_order", "confirm_order")

                    // 订单确认后的条件边（判断是否需要暂停）
                    .addConditionalEdges("confirm_order",
                            edge_async(this::routeAfterConfirmation),
                            Map.of(
                                    ConfirmTypeEnum.APPROVED.getCode(), "completion", // 成功也结束业务流程
                                    ConfirmTypeEnum.REJECTED.getCode(), "completion",
                                    END, END  // 等待用户时直接进入 END 节点，暂停
                            ))

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
     * 1. 如果是首次执行 (isResuming=false) -> 返回 "create_plan"，从第一个节点开始
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

        // 首次执行，从第一个节点开始
        log.info("首次执行：从 create_plan 开始");
        return "create_plan";
    }

    /**
     * 确认后的路由判断
     * <p>
     * 判断逻辑：
     * 1. 如果需要确认且确认结果为空 -> 返回 END，让工作流暂停，等待用户确认
     * 2. 如果确认结果为 approved -> 返回 "approved"，继续下一个节点
     * 3. 如果确认结果为 rejected -> 返回 "rejected"，跳转到完成节点，业务结束
     */
    private String routeAfterConfirmation(MessagesState<String> state) {
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
        
        log.info("确认路由判断: needConfirmation={}, confirmationResult={}, confirmationType={}", 
            context.isNeedConfirmation(), context.getConfirmationResult(), context.getConfirmationType());

        // 如果需要确认
        if (context.isNeedConfirmation()) {
            // 如果确认结果为空，说明用户还未确认，返回 END 暂停工作流
            if (StringUtils.isBlank(context.getConfirmationResult())) {
                log.info("需要用户确认 {}, 暂停工作流", context.getConfirmationType());
                return END;
            }
            
            // 如果用户已确认
            if (ConfirmTypeEnum.APPROVED.getCode().equalsIgnoreCase(context.getConfirmationResult())) {
                log.info("用户已确认 {}, 继续执行", context.getConfirmationType());

                // 重置确认状态，为下一次确认做准备
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                return ConfirmTypeEnum.APPROVED.getCode();
            }
            
            // 如果用户已拒绝
            if (ConfirmTypeEnum.REJECTED.getCode().equalsIgnoreCase(context.getConfirmationResult())) {
                log.info("用户已拒绝 {}, 跳转到完成节点", context.getConfirmationType());
                // 重置确认状态
                context.setNeedConfirmation(false);
                context.setConfirmationResult(null);
                return ConfirmTypeEnum.REJECTED.getCode();
            }
        }

        // 不需要确认，直接继续
        return ConfirmTypeEnum.APPROVED.getCode();
    }
}
