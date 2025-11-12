package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.agent.OrchestratorAgent;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.enums.ConfirmTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.enums.SSEEventTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 统筹节点（监督节点）
 * 该节点负责整体流程控制，检查游玩日期和人数，协调各个环节
 * 集成 AI 大模型来智能判断用户输入、处理流程
 */
@Slf4j
public class OrchestratorNode {

    private final WorkflowAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    public OrchestratorNode(WorkflowAgentFactory agentFactory, WorkflowCheckpointService checkpointService) {
        this.agentFactory = agentFactory;
        this.checkpointService = checkpointService;
    }

    /**
     * 创建统筹节点 - 检查日期和人数
     */
    public AsyncNodeAction<MessagesState<String>> createCheckInput() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
            log.info("执行节点: 统筹节点 - 检查用户输入");

            try {
                // 调用 AI 判断需要什么信息
                OrchestratorAgent agent = agentFactory.getOrchestratorAgent(context.getSessionId());
                String aiResponse = agent.checkAndPrompt(
                    context,
                    "请检查用户信息完整性"
                );
                
                log.info("AI 判断结果: {}", aiResponse);
                
                // 解析 AI 返回结果
                if (aiResponse.startsWith("NEED_DATE|")) {
                    // 需要输入日期
                    String promptMessage = aiResponse.substring("NEED_DATE|".length()).trim();
                    if (StringUtils.isBlank(promptMessage)) {
                        promptMessage = "📅 请提供游玩日期（格式：YYYY-MM-DD）";
                    }
                    
                    log.info("缺少游玩日期，AI 生成提示: {}", promptMessage);
                    
                    // 发送 INPUT_DATE 事件给前端
                    sendSseEvent(context.getSessionId(), checkpointService.getEmitter(context.getSessionId()), 
                            SSEEventTypeEnum.INPUT_DATE,
                            Map.of(
                                    "sseEventType", SSEEventTypeEnum.INPUT_DATE.getCode(),
                                    "content", promptMessage,
                                    "nodeName", "orchestrator_check",
                                    "confirmationType", "INPUT_DATE"
                            ));
                    
                    context.setCurrentStep(promptMessage);
                    context.setNeedConfirmation(true);
                    context.setConfirmationType("INPUT_DATE");
                    context.setPausedAtNode("orchestrator_check");
                    return ConfirmWorkflowContext.saveContext(context);
                    
                } else if (aiResponse.startsWith("NEED_COUNT|")) {
                    // 需要输入人数
                    String promptMessage = aiResponse.substring("NEED_COUNT|".length()).trim();
                    if (StringUtils.isBlank(promptMessage)) {
                        promptMessage = "👥 请提供游玩人数";
                    }
                    
                    log.info("缺少游玩人数，AI 生成提示: {}", promptMessage);
                    
                    // 发送 INPUT_COUNT 事件给前端
                    sendSseEvent(context.getSessionId(), checkpointService.getEmitter(context.getSessionId()), 
                            SSEEventTypeEnum.INPUT_COUNT,
                            Map.of(
                                    "sseEventType", SSEEventTypeEnum.INPUT_COUNT.getCode(),
                                    "content", promptMessage,
                                    "nodeName", "orchestrator_check",
                                    "confirmationType", "INPUT_COUNT"
                            ));
                    
                    context.setCurrentStep(promptMessage);
                    context.setNeedConfirmation(true);
                    context.setConfirmationType("INPUT_COUNT");
                    context.setPausedAtNode("orchestrator_check");
                    return ConfirmWorkflowContext.saveContext(context);
                    
                } else if (aiResponse.contains("COMPLETE")) {
                    // 信息完整，继续执行
                    log.info("游玩信息已完整: 日期={}, 人数={}", context.getVisitDate(), context.getVisitorCount());
                    
                    // 检查是否已有Plan
                    if (context.getPlan() == null) {
                        log.info("未找到Plan信息，需要创建行程");
                        context.setCurrentStep("游玩信息已完整，准备创建行程");
                        context.setNeedConfirmation(false);
                        return ConfirmWorkflowContext.saveContext(context);
                    }
                    
                    // 已有Plan，检查是否已有Order
                    if (context.getOrder() == null) {
                        log.info("已有Plan信息，但未找到Order信息，需要创建订单");
                        context.setCurrentStep("行程已创建，准备创建订单");
                        context.setNeedConfirmation(false);
                        return ConfirmWorkflowContext.saveContext(context);
                    }
                    
                    // Plan和Order都已创建，准备汇总
                    log.info("Plan和Order都已创建，准备生成汇总");
                    context.setCurrentStep("行程和订单都已创建，准备生成汇总");
                    context.setNeedConfirmation(false);
                    return ConfirmWorkflowContext.saveContext(context);
                    
                } else {
                    // AI 返回格式异常，使用备用逻辑
                    log.warn("AI 返回格式异常: {}，使用备用检查逻辑", aiResponse);
                    return fallbackCheck(context);
                }
                
            } catch (Exception e) {
                log.error("AI 判断失败，使用备用检查逻辑: {}", e.getMessage(), e);
                return fallbackCheck(context);
            }
        });
    }
    
    /**
     * 备用检查逻辑（当 AI 失败时使用）
     */
    private Map<String, Object> fallbackCheck(ConfirmWorkflowContext context) {
        log.info("使用备用检查逻辑");
        
        // 检查日期
        if (StringUtils.isBlank(context.getVisitDate())) {
            String promptMessage = "📅 请提供游玩日期（格式：YYYY-MM-DD）";
            log.info("缺少游玩日期");
            
            sendSseEvent(context.getSessionId(), checkpointService.getEmitter(context.getSessionId()), 
                    SSEEventTypeEnum.INPUT_DATE,
                    Map.of(
                            "sseEventType", SSEEventTypeEnum.INPUT_DATE.getCode(),
                            "content", promptMessage,
                            "nodeName", "orchestrator_check",
                            "confirmationType", "INPUT_DATE"
                    ));
            
            context.setCurrentStep(promptMessage);
            context.setNeedConfirmation(true);
            context.setConfirmationType("INPUT_DATE");
            context.setPausedAtNode("orchestrator_check");
            return ConfirmWorkflowContext.saveContext(context);
        }
        
        // 检查人数
        if (context.getVisitorCount() == null || context.getVisitorCount() <= 0) {
            String promptMessage = "👥 请提供游玩人数";
            log.info("缺少游玩人数");
            
            sendSseEvent(context.getSessionId(), checkpointService.getEmitter(context.getSessionId()), 
                    SSEEventTypeEnum.INPUT_COUNT,
                    Map.of(
                            "sseEventType", SSEEventTypeEnum.INPUT_COUNT.getCode(),
                            "content", promptMessage,
                            "nodeName", "orchestrator_check",
                            "confirmationType", "INPUT_COUNT"
                    ));
            
            context.setCurrentStep(promptMessage);
            context.setNeedConfirmation(true);
            context.setConfirmationType("INPUT_COUNT");
            context.setPausedAtNode("orchestrator_check");
            return ConfirmWorkflowContext.saveContext(context);
        }
        
        // 信息完整
        log.info("游玩信息已完整: 日期={}, 人数={}", context.getVisitDate(), context.getVisitorCount());
        
        if (context.getPlan() == null) {
            context.setCurrentStep("游玩信息已完整，准备创建行程");
        } else if (context.getOrder() == null) {
            context.setCurrentStep("行程已创建，准备创建订单");
        } else {
            context.setCurrentStep("行程和订单都已创建，准备生成汇总");
        }
        
        context.setNeedConfirmation(false);
        return ConfirmWorkflowContext.saveContext(context);
    }

    /**
     * 创建统筹节点 - 汇总完成信息
     */
    public AsyncNodeAction<MessagesState<String>> createSummary() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
            log.info("执行节点: 统筹节点 - 汇总完成信息");

            String summary;
            try {
                // 使用 AI 生成友好的完成汇总信息
                OrchestratorAgent agent = agentFactory.getOrchestratorAgent(context.getSessionId());
                summary = agent.generateSummary(
                    context.getVisitDate(),
                    context.getVisitorCount(),
                    context.getPlanId() != null ? context.getPlanId() : "未生成",
                    context.getOrderId() != null ? context.getOrderId() : "未生成",
                    "生成完成汇总"
                );
                log.info("AI 生成完成汇总成功，内容长度: {}", summary.length());
                
                // 发送 SSE 事件给前端
                sendSseEvent(context.getSessionId(), checkpointService.getEmitter(context.getSessionId()),
                        SSEEventTypeEnum.OUTPUT_CHUNK,
                        Map.of(
                                "sseEventType", SSEEventTypeEnum.OUTPUT_CHUNK.getCode(),
                                "content", summary,
                                "nodeName", "orchestrator_summary",
                                "confirmationType", "SUMMARY",
                                "planId", context.getPlanId() != null ? context.getPlanId() : "",
                                "orderId", context.getOrderId() != null ? context.getOrderId() : ""
                        ));
            } catch (Exception e) {
                log.warn("AI 生成汇总失败，使用默认模板: {}", e.getMessage());
                // 使用默认模板
                StringBuilder defaultSummary = new StringBuilder();
                defaultSummary.append("🎉 流程已完成！\n\n");
                defaultSummary.append("📅 游玩日期: ").append(context.getVisitDate()).append("\n");
                defaultSummary.append("👥 游玩人数: ").append(context.getVisitorCount()).append("人\n\n");

                if (StringUtils.isNotBlank(context.getPlanId())) {
                    defaultSummary.append("📋 行程ID: ").append(context.getPlanId()).append("\n");
                }

                if (StringUtils.isNotBlank(context.getOrderId())) {
                    defaultSummary.append("🛍️ 订单ID: ").append(context.getOrderId()).append("\n");
                }

                defaultSummary.append("\n✨ 感谢您的使用，祝您玩得开心！");
                summary = defaultSummary.toString();
            }

            context.setCurrentStep(summary);
            log.info("工作流完成汇总");

            return ConfirmWorkflowContext.saveContext(context);
        });
    }

    /**
     * 发送 SSE 事件
     */
    private void sendSseEvent(String sessionId, SseEmitter emitter, SSEEventTypeEnum eventType, Object data) {
        if (emitter == null) {
            log.warn("跳过SSE事件发送，Emitter为null: eventType={}, sessionId={}", eventType.getCode(), sessionId);
            return;
        }
        
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            log.info("[SSE]📤 ========== 发送SSE事件 ==========");
            log.info("[SSE]📍 sessionId: {}", sessionId);
            log.info("[SSE]📍 eventType: {}", eventType.getCode());
            log.info("[SSE]📍 数据: {}", jsonData);
            log.info("[SSE]📦 SSE格式: event:{}", eventType.getCode());
            log.info("[SSE]📦 SSE格式: data:{}", jsonData);

            emitter.send(SseEmitter.event()
                    .name(eventType.getCode())
                    .data(jsonData));

            log.info("[SSE]✅ SSE事件发送成功: eventType={}", eventType.getCode());
        } catch (IOException e) {
            log.error("[SSE]❌ 发送SSE事件失败: eventType={}, error={}", eventType.getCode(), e.getMessage(), e);
        }
    }
}

