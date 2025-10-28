package com.unicorn.journey.assistant.langgragh4j.service;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.StreamConfirmWorkflowApp;
import com.unicorn.journey.assistant.langgragh4j.enums.ConfirmTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowHandleService {
    private final UserService userService;
    private final WorkflowCheckpointService checkpointService;
    

    /**
     * 执行工作流（首次启动）
     */
    public void executeWorkflow(String sessionId, String userId, SseEmitter emitter) {
        try {
            log.info("首次启动工作流: sessionId={}, userId={}", sessionId, userId);

            // 发送开始事件
            sendSseEvent(sessionId, emitter, "workflow_start", Map.of(
                    "message", "启动工作流",
                    "sessionId", sessionId,
                    "userId", userId
            ));

            log.info("workflow_start 事件已发送");

            // 创建工作流
            log.info("开始创建工作流...");
            StreamConfirmWorkflowApp app = new StreamConfirmWorkflowApp();
            CompiledGraph<MessagesState<String>> workflow = app.createWorkflow();
            log.info("工作流图:/n{}", workflow.getGraph(GraphRepresentation.Type.MERMAID).content());

            // 获取用户信息
            User user = userService.currentUser();
            log.info("获取到当前用户: {}-{}", user.getId(), user.getNickname());

            // 初始化上下文
            ConfirmWorkflowContext initialContext = ConfirmWorkflowContext.builder()
                    .currentStep("初始化")
                    .user(user)
                    .sessionId(sessionId)
                    .build();

            // 执行工作流并处理结果
            runWorkflow(workflow, initialContext, sessionId, emitter);

        } catch (Exception e) {
            log.error("工作流执行失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            sendSseEvent(sessionId, emitter, "workflow_error", Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
                    "message", "工作流执行失败"
            ));
            try {
                emitter.completeWithError(e);
            } catch (Exception completeError) {
                log.error("完成emitter失败:", completeError);
            }
            checkpointService.cleanupSession(sessionId);
        }
    }

    /**
     * 运行工作流
     */
    private void runWorkflow(CompiledGraph<MessagesState<String>> workflow,
                             ConfirmWorkflowContext context,
                             String sessionId,
                             SseEmitter emitter) {
        try {
            // 初始化工作流最终状态
            MessagesState<String> lastState = null;
            int stepCounter = 1;
            int nodeCounter = 0;

            log.info("=== 开始执行工作流 ===");
            
            // 流式执行工作流
            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    ConfirmWorkflowContext.saveContext(context))) {

                nodeCounter++;
                
                // 跳过 START 和 END
                if (step.isSTART() || step.isEND()) {
                    continue;
                }

                // 获取最新状态的上下文
                lastState = step.state();
                ConfirmWorkflowContext currentContext = ConfirmWorkflowContext.getContext(lastState);

                if (currentContext != null) {
                    String nodeName = step.node();

                    log.info("当前步骤: {}", currentContext.getCurrentStep());

                    String currentStepInfo = currentContext.getCurrentStep();
                    sendSseEvent(sessionId, emitter, "step_update", Map.of(
                            "nodeName", nodeName,
                            "currentStep", currentStepInfo != null ? currentStepInfo : "",
                            "needConfirmation", currentContext.isNeedConfirmation(),
                            "confirmationType", currentContext.getConfirmationType() != null ?
                                    currentContext.getConfirmationType() : ""
                    ));

                    log.info("节点 {} 处理完成, 已发送 step_update 事件", nodeName);
                }
            }

            // 工作流执行完成后的处理
            if (lastState != null) {
                ConfirmWorkflowContext finalContext = ConfirmWorkflowContext.getContext(lastState);

                log.info("工作流执行结束检查: needConfirmation={}, confirmationResult={}, confirmationType={}",
                        finalContext.isNeedConfirmation(),
                        finalContext.getConfirmationResult(),
                        finalContext.getConfirmationType());

                // 如果上下文状态中，需要确认且确认结果为空，说明工作流暂停等待用户确认
                if (finalContext.isNeedConfirmation() &&
                        StringUtils.isBlank(finalContext.getConfirmationResult())) {

                    log.info("工作流暂停，等待用户确认: type={}", finalContext.getConfirmationType());

                    // 保存暂停状态
                    checkpointService.savePausedState(sessionId, lastState);

                    // 获取当前步骤的详细信息
                    String currentStepDetails = finalContext.getCurrentStep();

                    // 发送确认请求事件
                    sendSseEvent(sessionId, emitter, "confirmation_required", Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId()!= null ? finalContext.getOrderId() : "",
                            "details", currentStepDetails != null ? currentStepDetails : "",
                            "message", "PLAN".equals(finalContext.getConfirmationType()) ?
                                    "请确认行程信息" : "请确认订单信息"
                    ));

                    log.info("已发送确认请求事件: sessionId={}, type={}",
                            sessionId, finalContext.getConfirmationType());

                } else {
                    // 工作流正常结束
                    log.info("工作流正常结束");
                    sendSseEvent(sessionId, emitter, "workflow_completed", Map.of(
                            "message", "工作流执行完成",
                            "finalStep", finalContext.getCurrentStep(),
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId() != null ? finalContext.getOrderId() : ""
                    ));

                    log.info("工作流执行完成: sessionId={}", sessionId);
                    emitter.complete();
                    checkpointService.cleanupSession(sessionId);
                }
            }

        } catch (Exception e) {
            log.error("运行工作流失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 恢复工作流执行（用户确认后）
     */
    public void resumeWorkflow(String sessionId, MessagesState<String> pausedState, SseEmitter emitter) {
        try {
            // 创建工作流
            StreamConfirmWorkflowApp app = new StreamConfirmWorkflowApp();
            CompiledGraph<MessagesState<String>> workflow = app.createWorkflow();

            log.info("恢复工作流图:\n {}", workflow.getGraph(GraphRepresentation.Type.MERMAID).content());

            // 获取更新后的上下文
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(pausedState);

            // 标记为恢复执行模式，恢复路由节点会根据此标志进行跳转
            context.setResuming(true);

            log.info("恢复工作流执行: sessionId={}, confirmationResult={}, pausedAtNode={}, isResuming={}",
                    sessionId, context.getConfirmationResult(), context.getPausedAtNode(), context.isResuming());

            // 发送确认结果事件（包含具体的确认类型）
            /*boolean isApproved = ConfirmTypeEnum.APPROVED.getCode().equals(context.getConfirmationResult());
            String confirmationType = context.getConfirmationType();
            String itemName = "PLAN".equals(confirmationType) ? "行程计划" :
                            "ORDER".equals(confirmationType) ? "订单" : "操作";

            sendSseEvent(sessionId, emitter, "confirmation_result", Map.of(
                    "confirmed", isApproved,
                    "confirmationType", confirmationType != null ? confirmationType : "",
                    "message", isApproved ?
                            String.format("您已确认%s，工作流继续执行", itemName) :
                            String.format("您已拒绝%s", itemName)
            ));*/


            // 直接从暂停节点继续
            resumeWorkflowFromPausedNode(workflow, pausedState, sessionId, emitter);

        } catch (Exception e) {
            log.error("恢复工作流失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            sendSseEvent(sessionId, emitter, "workflow_error", Map.of(
                    "error", e.getMessage(),
                    "message", "恢复工作流失败"
            ));
            emitter.completeWithError(e);
            checkpointService.cleanupSession(sessionId);
        }
    }

    /**
     * 从暂停节点恢复执行工作流（使用条件边智能路由）
     * <p>
     * 1. context.isResuming = true 标记为恢复模式
     * 2. context.pausedAtNode 记录暂停的节点
     * 3. resume_router 节点会根据这两个字段，通过条件边直接跳转到暂停节点
     */
    private void resumeWorkflowFromPausedNode(CompiledGraph<MessagesState<String>> workflow,
                                              MessagesState<String> pausedState,
                                              String sessionId,
                                              SseEmitter emitter) {
        try {
            // 工作流最终状态
            MessagesState<String> lastState = pausedState;

            log.info("=== 从暂停节点恢复执行工作流: sessionId={} ===", sessionId);

            // resume_router 节点会通过条件边判断，直接跳转到 pausedAtNode
            for (NodeOutput<MessagesState<String>> step : workflow.stream(pausedState.data())) {

                // 跳过 START 和 END
                if (step.isSTART() || step.isEND()) {
                    continue;
                }

                lastState = step.state();
                ConfirmWorkflowContext currentContext = ConfirmWorkflowContext.getContext(lastState);

                if (currentContext != null) {
                    String nodeName = step.node();

                    log.info("当前步骤: {}", currentContext.getCurrentStep());

                    String currentStepInfo = currentContext.getCurrentStep();
                    sendSseEvent(sessionId, emitter, "step_update", Map.of(
                            "nodeName", nodeName,
                            "currentStep", currentStepInfo != null ? currentStepInfo : "",
                            "needConfirmation", currentContext.isNeedConfirmation(),
                            "confirmationType", currentContext.getConfirmationType() != null ?
                                    currentContext.getConfirmationType() : ""
                    ));

                    log.info("节点 {} 处理完成, 已发送 step_update 事件", nodeName);
                }
            }
            
            log.info("===  恢复执行完成！ ===");

            // 工作流恢复执行完成后的处理
            if (lastState != null) {
                ConfirmWorkflowContext finalContext = ConfirmWorkflowContext.getContext(lastState);

                log.info("工作流执行结束检查: needConfirmation={}, confirmationResult={}, confirmationType={}",
                        finalContext.isNeedConfirmation(),
                        finalContext.getConfirmationResult(),
                        finalContext.getConfirmationType());

                // 如果上下文中，需要确认且确认结果为空，说明工作流再次暂停等待用户确认
                if (finalContext.isNeedConfirmation() &&
                        StringUtils.isBlank(finalContext.getConfirmationResult())) {

                    log.info("工作流再次暂停，等待用户确认: type={}", finalContext.getConfirmationType());

                    // 重置恢复标志
                    finalContext.setResuming(false);

                    // 保存暂停状态（先保存状态，确保不会丢失）
                    checkpointService.savePausedState(sessionId, lastState);

                    // 获取当前步骤的详细信息
                    String currentStepDetails = finalContext.getCurrentStep();

                    log.info("工作流需要二次暂停： sessionId={}, 当前步骤: {}", sessionId, finalContext.getCurrentStep());
                    // 发送确认请求事件，包含详细信息mock数据
                    sendSseEvent(sessionId, emitter, "confirmation_required", Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId()!= null ? finalContext.getOrderId() : "",
                            "details", currentStepDetails != null ? currentStepDetails : "",
                            "message", "PLAN".equals(finalContext.getConfirmationType()) ?
                                    "请确认行程信息" : "请确认订单信息"
                    ));

                    log.info("已发送确认请求事件到前端（二次暂停）: sessionId={}, type={}",
                            sessionId, finalContext.getConfirmationType());

                } else {
                    // 工作流正常结束
                    log.info("工作流正常结束");
                    sendSseEvent(sessionId, emitter, "workflow_completed", Map.of(
                            "message", "工作流执行完成",
                            "finalStep", finalContext.getCurrentStep(),
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId() != null ? finalContext.getOrderId() : ""
                    ));

                    log.info("工作流执行完成: sessionId={}", sessionId);
                    emitter.complete();
                    checkpointService.cleanupSession(sessionId);
                }
            }

        } catch (Exception e) {
            log.error("恢复运行工作流失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 发送 SSE 事件
     */
    private void sendSseEvent(String sessionId, SseEmitter emitter, String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            log.info("发送SSE事件: eventType={}, sessionId={}", eventType, sessionId);
            
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(jsonData));
            
            log.info("SSE事件发送成功: eventType={}", eventType);
        } catch (IOException e) {
            log.error("发送SSE事件失败: eventType={}, error={}", eventType, e.getMessage(), e);
        }
    }

}
