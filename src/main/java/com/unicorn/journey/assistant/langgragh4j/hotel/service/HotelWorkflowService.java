package com.unicorn.journey.assistant.langgragh4j.hotel.service;

import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.hotel.HotelAssistantWorkflowApp;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.tool.RoomTypeQueryTool;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.service.UserService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 酒店工作流服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelWorkflowService {

    private final UserService userService;
    private final WorkflowCheckpointService checkpointService;
    private final HotelAgentFactory agentFactory;
    private final RoomTypeQueryTool roomTypeQueryTool;

    /**
     * 执行工作流
     */
    public void executeWorkflow(String sessionId, String userId, String userMessage, SseEmitter emitter) {
        try {
            log.info("启动酒店助手工作流: sessionId={}, userId={}, userMessage={}", sessionId, userId, userMessage);

            // 发送开始事件
            HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.WORKFLOW_START, Map.of(
                    "message", "酒店助手已启动",
                    "sessionId", sessionId,
                    "userId", userId
            ));

            // 保存 SSE 连接
            checkpointService.saveEmitter(sessionId, emitter);

            // 创建工作流
            HotelAssistantWorkflowApp app = new HotelAssistantWorkflowApp(agentFactory, checkpointService, roomTypeQueryTool);
            CompiledGraph<MessagesState<String>> workflow = app.createWorkflow();
            log.info("工作流图:\n{}", workflow.getGraph(GraphRepresentation.Type.MERMAID).content());

            // 获取用户信息
            User user = userService.currentUser();

            // 初始化上下文
            HotelAssistantContext initialContext = HotelAssistantContext.builder()
                    .currentStep("初始化")
                    .user(user)
                    .sessionId(sessionId)
                    .userMessage(userMessage)
                    .build();
            
            // 记录用户输入到对话历史
            initialContext.addUserInput(userMessage);
            log.info("记录用户输入: {}", userMessage);

            // 执行工作流
            runWorkflow(workflow, initialContext, sessionId, emitter);

        } catch (Exception e) {
            log.error("工作流执行失败", e);
            HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.WORKFLOW_ERROR, Map.of(
                    "error", e.getMessage(),
                    "message", "工作流执行失败"
            ));
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("完成emitter失败", ex);
            }
            checkpointService.cleanupSession(sessionId);
        }
    }

    /**
     * 运行工作流
     */
    private void runWorkflow(CompiledGraph<MessagesState<String>> workflow,
                             HotelAssistantContext context,
                             String sessionId,
                             SseEmitter emitter) {
        try {
            MessagesState<String> lastState = null;

            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    HotelAssistantContext.saveContext(context))) {

                if (step.isSTART() || step.isEND()) {
                    continue;
                }

                lastState = step.state();
                HotelAssistantContext currentContext = HotelAssistantContext.getContext(lastState);

                if (currentContext != null) {
                    String nodeName = step.node();
                    log.info("当前步骤: {}", currentContext.getCurrentStep());

                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.STEP_UPDATE, Map.of(
                            "nodeName", nodeName,
                            "currentStep", currentContext.getCurrentStep() != null ? currentContext.getCurrentStep() : "",
                            "needConfirmation", currentContext.isNeedConfirmation()
                    ));
                }
            }

            // 工作流执行完成后的处理
            if (lastState != null) {
                HotelAssistantContext finalContext = HotelAssistantContext.getContext(lastState);

                // 如果需要用户确认/输入
                if (finalContext.isNeedConfirmation() && StringUtils.isBlank(finalContext.getConfirmationResult())) {
                    log.info("工作流暂停，等待用户操作: type={}", finalContext.getConfirmationType());
                    checkpointService.savePausedState(sessionId, lastState);

                    // 发送确认请求事件
                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.CONFIRMATION_REQUIRED, Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "message", finalContext.getCurrentStep() != null ? finalContext.getCurrentStep() : "请确认"
                    ));
                } else {
                    // 工作流正常结束
                    log.info("工作流正常结束");
                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.WORKFLOW_COMPLETE, Map.of(
                            "message", "服务完成",
                            "summary", finalContext.getSummary() != null ? finalContext.getSummary() : ""
                    ));

                    emitter.complete();
                    checkpointService.cleanupSession(sessionId);
                }
            }

        } catch (Exception e) {
            log.error("运行工作流失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 恢复工作流
     */
    public void resumeWorkflow(String sessionId, MessagesState<String> pausedState, SseEmitter emitter) {
        try {
            HotelAssistantWorkflowApp app = new HotelAssistantWorkflowApp(agentFactory, checkpointService, roomTypeQueryTool);
            CompiledGraph<MessagesState<String>> workflow = app.createWorkflow();

            HotelAssistantContext context = HotelAssistantContext.getContext(pausedState);
            context.setResuming(true);

            log.info("恢复工作流执行: sessionId={}, pausedAtNode={}", sessionId, context.getPausedAtNode());

            resumeWorkflowFromPausedNode(workflow, pausedState, sessionId, emitter);

        } catch (Exception e) {
            log.error("恢复工作流失败", e);
            HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.WORKFLOW_ERROR, Map.of(
                    "error", e.getMessage(),
                    "message", "恢复工作流失败"
            ));
            emitter.completeWithError(e);
            checkpointService.cleanupSession(sessionId);
        }
    }

    /**
     * 从暂停节点恢复执行
     */
    private void resumeWorkflowFromPausedNode(CompiledGraph<MessagesState<String>> workflow,
                                              MessagesState<String> pausedState,
                                              String sessionId,
                                              SseEmitter emitter) {
        try {
            MessagesState<String> lastState = pausedState;

            for (NodeOutput<MessagesState<String>> step : workflow.stream(pausedState.data())) {
                if (step.isSTART() || step.isEND()) {
                    continue;
                }

                lastState = step.state();
                HotelAssistantContext currentContext = HotelAssistantContext.getContext(lastState);

                if (currentContext != null) {
                    String nodeName = step.node();
                    log.info("当前步骤: {}", currentContext.getCurrentStep());

                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.STEP_UPDATE, Map.of(
                            "nodeName", nodeName,
                            "currentStep", currentContext.getCurrentStep() != null ? currentContext.getCurrentStep() : ""
                    ));
                }
            }

            // 工作流执行完成后的处理
            if (lastState != null) {
                HotelAssistantContext finalContext = HotelAssistantContext.getContext(lastState);

                if (finalContext.isNeedConfirmation() && StringUtils.isBlank(finalContext.getConfirmationResult())) {
                    log.info("工作流再次暂停");
                    finalContext.setResuming(false);
                    checkpointService.savePausedState(sessionId, lastState);

                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.CONFIRMATION_REQUIRED, Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "message", finalContext.getCurrentStep() != null ? finalContext.getCurrentStep() : "请确认"
                    ));
                } else {
                    log.info("工作流正常结束");
                    HotelSseHelper.sendSseEvent(sessionId, emitter, HotelSSEEventType.WORKFLOW_COMPLETE, Map.of(
                            "message", "服务完成",
                            "summary", finalContext.getSummary() != null ? finalContext.getSummary() : ""
                    ));

                    emitter.complete();
                    checkpointService.cleanupSession(sessionId);
                }
            }

        } catch (Exception e) {
            log.error("恢复运行工作流失败", e);
            throw new RuntimeException(e);
        }
    }
}
