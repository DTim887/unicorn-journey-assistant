package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.agent.OrchestratorAgent;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 统筹节点
 * 该节点负责整体流程控制，检查游玩日期和人数，协调各个环节
 * 集成 AI 大模型来智能判断和处理用户输入
 */
@Slf4j
public class OrchestratorNode {

    private final WorkflowAgentFactory agentFactory;

    public OrchestratorNode(WorkflowAgentFactory agentFactory) {
        this.agentFactory = agentFactory;
    }

    /**
     * 创建统筹节点 - 检查日期和人数
     */
    public AsyncNodeAction<MessagesState<String>> createCheckInput() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
            log.info("执行节点: 统筹节点 - 检查用户输入");

            // 检查是否有游玩日期
            if (StringUtils.isBlank(context.getVisitDate())) {
                log.info("缺少游玩日期，需要用户提供");
                
                // 使用 AI 生成友好的提示（可选，如果 AI 不可用则使用默认提示）
                String promptMessage = "请提供游玩日期（格式：YYYY-MM-DD）";
                try {
                    OrchestratorAgent agent = agentFactory.getOrchestratorAgent(context.getSessionId());
                    String aiPrompt = agent.checkInput(null, context.getVisitorCount(), "需要游玩日期");
                    if (StringUtils.isNotBlank(aiPrompt) && !aiPrompt.contains("信息完整")) {
                        promptMessage = aiPrompt;
                    }
                } catch (Exception e) {
                    log.warn("AI 生成提示失败，使用默认提示: {}", e.getMessage());
                }
                
                context.setCurrentStep(promptMessage);
                context.setNeedConfirmation(true);
                context.setConfirmationType("INPUT_DATE");
                context.setPausedAtNode("orchestrator_check");
                return ConfirmWorkflowContext.saveContext(context);
            }

            // 检查是否有游玩人数
            if (context.getVisitorCount() == null || context.getVisitorCount() <= 0) {
                log.info("缺少游玩人数，需要用户提供");
                
                // 使用 AI 生成友好的提示（可选）
                String promptMessage = "请提供游玩人数";
                try {
                    OrchestratorAgent agent = agentFactory.getOrchestratorAgent(context.getSessionId());
                    String aiPrompt = agent.checkInput(context.getVisitDate(), null, "需要游玩人数");
                    if (StringUtils.isNotBlank(aiPrompt) && !aiPrompt.contains("信息完整")) {
                        promptMessage = aiPrompt;
                    }
                } catch (Exception e) {
                    log.warn("AI 生成提示失败，使用默认提示: {}", e.getMessage());
                }
                
                context.setCurrentStep(promptMessage);
                context.setNeedConfirmation(true);
                context.setConfirmationType("INPUT_COUNT");
                context.setPausedAtNode("orchestrator_check");
                return ConfirmWorkflowContext.saveContext(context);
            }

            // 日期和人数都齐全
            log.info("游玩信息已完整: 日期={}, 人数={}", context.getVisitDate(), context.getVisitorCount());
            context.setCurrentStep("游玩信息已完整，准备创建行程");
            context.setNeedConfirmation(false);

            return ConfirmWorkflowContext.saveContext(context);
        });
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
                log.info("AI 生成完成汇总成功");
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
}

