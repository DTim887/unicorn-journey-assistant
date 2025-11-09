package com.unicorn.journey.assistant.langgragh4j.service;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.StreamConfirmWorkflowApp;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.enums.SSEEventTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.UserService;
import com.unicorn.journey.assistant.service.PlanService;
import com.unicorn.journey.assistant.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowHandleService {
    private final UserService userService;
    private final WorkflowCheckpointService checkpointService;
    private final WorkflowAgentFactory agentFactory;
    private final PlanService planService;
    private final OrderService orderService;
    

    /**
     * 执行工作流（首次启动）
     */
    public void executeWorkflow(String sessionId, String userId, String visitDate, Integer visitorCount, SseEmitter emitter) {
        try {
            log.info("首次启动工作流: sessionId={}, userId={}, visitDate={}, visitorCount={}", 
                sessionId, userId, visitDate, visitorCount);

            // 发送开始事件
            sendSseEvent(sessionId, emitter, SSEEventTypeEnum.WORKFLOW_START, Map.of(
                    "message", "启动工作流",
                    "sessionId", sessionId,
                    "userId", userId
            ));

            log.info("workflow_start 事件已发送");

            // 创建工作流
            log.info("开始创建工作流...");
            StreamConfirmWorkflowApp app = new StreamConfirmWorkflowApp(agentFactory, planService, orderService);
            CompiledGraph<MessagesState<String>> workflow = app.createWorkflow();
            log.info("工作流图:\n{}", workflow.getGraph(GraphRepresentation.Type.MERMAID).content());

            // 获取用户信息
            User user = userService.currentUser();
            log.info("获取到当前用户: {}-{}", user.getId(), user.getNickname());

            // 初始化上下文，包含日期和人数（如果提供）
            ConfirmWorkflowContext initialContext = ConfirmWorkflowContext.builder()
                    .currentStep("初始化")
                    .user(user)
                    .sessionId(sessionId)
                    .visitDate(visitDate)  // 设置游玩日期
                    .visitorCount(visitorCount)  // 设置游玩人数
                    .build();

            // 执行工作流并处理结果
            runWorkflow(workflow, initialContext, sessionId, emitter);

        } catch (Exception e) {
            log.error("工作流执行失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            sendSseEvent(sessionId, emitter, SSEEventTypeEnum.WORKFLOW_ERROR, Map.of(
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

            log.info("=== 开始执行工作流 ===");
            
            // 流式执行工作流
            for (NodeOutput<MessagesState<String>> step : workflow.stream(
                    ConfirmWorkflowContext.saveContext(context))) {
                
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

                    // 如果是create_plan或create_order节点，则流式发送详细信息
                    if ("create_plan".equals(nodeName) || "create_order".equals(nodeName)) {
                        log.info("检测到{}节点，开始流式发送详细信息", nodeName);
                        sendDetailsAsFlux(sessionId, emitter, currentContext, nodeName);
                    }

                    String currentStepInfo = currentContext.getCurrentStep();
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.STEP_UPDATE, Map.of(
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

                    // 根据确认类型生成提示消息
                    String message = switch (finalContext.getConfirmationType()) {
                        case "INPUT_DATE" -> "请提供游玩日期";
                        case "INPUT_COUNT" -> "请提供游玩人数";
                        case "PLAN" -> "请确认行程信息";
                        case "ORDER" -> "请确认订单信息";
                        default -> "请确认";
                    };
                    
                    // 发送确认请求事件
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.CONFIRMATION_REQUIRED, Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId()!= null ? finalContext.getOrderId() : "",
                            "details", currentStepDetails != null ? currentStepDetails : "",
                            "message", message
                    ));

                    log.info("已发送确认请求事件: sessionId={}, type={}",
                            sessionId, finalContext.getConfirmationType());

                } else {
                    // 工作流正常结束
                    log.info("工作流正常结束");
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.WORKFLOW_COMPLETE, Map.of(
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
            StreamConfirmWorkflowApp app = new StreamConfirmWorkflowApp(agentFactory, planService, orderService);
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
            sendSseEvent(sessionId, emitter, SSEEventTypeEnum.WORKFLOW_ERROR, Map.of(
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

                    // 如果是create_plan或create_order节点，则流式发送详细信息
                    if ("create_plan".equals(nodeName) || "create_order".equals(nodeName)) {
                        log.info("检测到{}节点，开始流式发送详细信息", nodeName);
                        sendDetailsAsFlux(sessionId, emitter, currentContext, nodeName);
                    }

                    String currentStepInfo = currentContext.getCurrentStep();
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.STEP_UPDATE, Map.of(
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

                    // 根据确认类型生成提示消息
                    String resumeMessage = switch (finalContext.getConfirmationType()) {
                        case "INPUT_DATE" -> "请提供游玩日期";
                        case "INPUT_COUNT" -> "请提供游玩人数";
                        case "PLAN" -> "请确认行程信息";
                        case "ORDER" -> "请确认订单信息";
                        default -> "请确认";
                    };
                    
                    // 发送确认请求事件，包含详细信息mock数据
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.CONFIRMATION_REQUIRED, Map.of(
                            "confirmationType", finalContext.getConfirmationType() != null ? finalContext.getConfirmationType() : "",
                            "planId", finalContext.getPlanId() != null ? finalContext.getPlanId() : "",
                            "orderId", finalContext.getOrderId()!= null ? finalContext.getOrderId() : "",
                            "details", currentStepDetails != null ? currentStepDetails : "",
                            "message", resumeMessage
                    ));

                    log.info("已发送确认请求事件到前端（二次暂停）: sessionId={}, type={}",
                            sessionId, finalContext.getConfirmationType());

                } else {
                    // 工作流正常结束
                    log.info("工作流正常结束");
                    sendSseEvent(sessionId, emitter, SSEEventTypeEnum.WORKFLOW_COMPLETE, Map.of(
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
    private void sendSseEvent(String sessionId, SseEmitter emitter, SSEEventTypeEnum eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            log.info("发送SSE事件: eventType={}, sessionId={}", eventType.getCode(), sessionId);
            
            emitter.send(SseEmitter.event()
                    .name(eventType.getCode())
                    .data(jsonData));
            
            log.info("SSE事件发送成功: eventType={}", eventType.getCode());
        } catch (IOException e) {
            log.error("发送SSE事件失败: eventType={}, error={}", eventType.getCode(), e.getMessage(), e);
        }
    }

    /**
     * 根据节点类型从currentStep中提取details并流式发送
     * 
     * @param sessionId 会话ID
     * @param emitter SSE发送器
     * @param context 工作流上下文
     * @param nodeName 节点名称
     */
    private void sendDetailsAsFlux(String sessionId, SseEmitter emitter, ConfirmWorkflowContext context, String nodeName) {
        String currentStep = context.getCurrentStep();
        if (currentStep == null || currentStep.isEmpty()) {
            log.warn("currentStep为空，无法生成Flux: sessionId={}, nodeName={}", sessionId, nodeName);
            return;
        }

        // 从currentStep中提取details（去除前缀）
        String details = "";
        if ("create_plan".equals(nodeName) && currentStep.startsWith("创建行程")) {
            details = currentStep.substring("创建行程".length());
        } else if ("create_order".equals(nodeName) && currentStep.startsWith("创建订单完成")) {
            details = currentStep.substring("创建订单完成".length());
        }

        if (details.isEmpty()) {
            log.warn("无法提取details: sessionId={}, nodeName={}", sessionId, nodeName);
            return;
        }

        // 将details按行分块
        String[] lines = details.split("\n");
        List<String> chunks = new java.util.ArrayList<>();
        
        // 将每2-3行合并为一个chunk，模拟流式输出
        StringBuilder chunkBuilder = new StringBuilder();
        int lineCount = 0;
        for (String line : lines) {
            chunkBuilder.append(line).append("\n");
            lineCount++;
            if (lineCount >= 2) {
                chunks.add(chunkBuilder.toString());
                chunkBuilder = new StringBuilder();
                lineCount = 0;
            }
        }
        // 添加剩余的行
        if (chunkBuilder.length() > 0) {
            chunks.add(chunkBuilder.toString());
        }

        log.info("开始流式发送details: sessionId={}, nodeName={}, chunks数量={}", sessionId, nodeName, chunks.size());

        // 创建Flux并订阅发送
        Flux<String> detailsFlux = Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(200));

        // 使用CountDownLatch等待Flux完成
        CountDownLatch latch = new CountDownLatch(1);
        
        // 订阅Flux并发送每个chunk
        detailsFlux.subscribe(
                chunk -> {
                    // 发送每个chunk
                    try {
                        sendSseEvent(sessionId, emitter, SSEEventTypeEnum.OUTPUT_CHUNK, Map.of(
                                "chunk", chunk,
                                "nodeName", nodeName,
                                "confirmationType", "create_plan".equals(nodeName) ? "PLAN" : "ORDER",
                                "planId", context.getPlanId() != null ? context.getPlanId() : "",
                                "orderId", context.getOrderId() != null ? context.getOrderId() : ""
                        ));
                        log.debug("Flux chunk已发送: sessionId={}, chunk长度={}", sessionId, chunk.length());
                    } catch (Exception e) {
                        log.error("发送Flux chunk失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                    }
                },
                error -> {
                    // 错误处理
                    log.error("Flux订阅出错: sessionId={}, error={}", sessionId, error.getMessage(), error);
                    latch.countDown();
                },
                () -> {
                    // 完成处理
                    log.info("Flux流式发送完成: sessionId={}, nodeName={}", sessionId, nodeName);
                    latch.countDown();
                }
        );

        // 等待Flux完成（最多等待30秒）
        try {
            boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Flux流式发送超时: sessionId={}", sessionId);
            }
        } catch (InterruptedException e) {
            log.error("等待Flux完成时被中断: sessionId={}", sessionId, e);
            Thread.currentThread().interrupt();
        }

        log.info("Details流式输出完成: sessionId={}, nodeName={}", sessionId, nodeName);
    }

}
