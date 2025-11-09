package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowPlanAgent;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建行程节点
 * 该节点专门负责创建行程，不包含确认逻辑
 * 集成 AI 大模型生成个性化行程
 */
@Slf4j
public class CreatePlanWithConfirmNode {

    private final WorkflowAgentFactory agentFactory;
    private final PlanService planService;

    public CreatePlanWithConfirmNode(WorkflowAgentFactory agentFactory, PlanService planService) {
        this.agentFactory = agentFactory;
        this.planService = planService;
    }

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 创建行程 - AI生成");

            // 检查用户是否已有 plan
            if (context.getUser() != null) {
                Plan existingPlan = planService.retrievePlanByUserId(context.getUser().getId());
                if (existingPlan != null) {
                    log.info("用户已存在 plan: planId={}, userId={}", existingPlan.getId(), context.getUser().getId());
                    context.setPlan(existingPlan);
                    context.setPlanId("PLAN-" + existingPlan.getId());
                    context.setCurrentStep("检测到用户已有行程，将使用现有行程");
                    return ConfirmWorkflowContext.saveContext(context);
                }
            }

            // 生成行程ID
            String planId = "PLAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            context.setPlanId(planId);

            try {
                // 使用 AI Agent 生成行程
                WorkflowPlanAgent planAgent = agentFactory.getPlanAgent(context.getSessionId());
                
                String userMessage = StringUtils.isNotBlank(context.getUserMessage()) 
                    ? context.getUserMessage() 
                    : "请为我创建一份详细的上海迪士尼乐园游玩行程";
                
                log.info("调用 AI 生成行程: visitDate={}, visitorCount={}", 
                    context.getVisitDate(), context.getVisitorCount());
                
                String planContent = planAgent.createPlan(
                    context.getVisitDate(),
                    context.getVisitorCount(),
                    userMessage
                );

                // 保存 AI 生成的行程内容
                context.setCurrentStep("创建行程完成\n\n行程ID: " + planId + "\n\n" + planContent);
                log.info("AI 行程生成完成，行程ID: {}", planId);

            } catch (Exception e) {
                log.error("AI 生成行程失败", e);
                //String defaultPlan = generateDefaultPlan(context);
                context.setCurrentStep("创建行程失败");
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }

   /* *//**
     * 生成默认行程模板（AI 失败时的备选方案）
     *//*
    private String generateDefaultPlan(ConfirmWorkflowContext context) {
        return String.format("""
            🎉 上海迪士尼乐园一日游行程
            
            📅 游玩日期：%s
            👥 游客人数：%d人
            
            🗺️ 推荐路线：
            1. 08:00-08:30 入园，前往探险岛
            2. 08:30-10:00 飞越地平线（FastPass推荐）
            3. 10:00-11:00 雷鸣山漂流
            4. 11:30-12:30 午餐：巴波萨烧烤
            5. 13:00-14:00 宝藏湾-加勒比海盗
            6. 14:30-16:00 创极速光轮
            7. 16:30-17:30 七个小矮人矿山车
            8. 17:30-18:30 晚餐：皇家宴会厅
            9. 19:00-19:30 观赏奇幻童话城堡
            10. 19:30-20:00 烟花表演
            
            💰 预估费用：
            - 门票：¥599/人 × %d人 = ¥%d
            - 餐饮：¥200/人 × %d人 = ¥%d
            - 总计：¥%d
            
            💡 温馨提示：
            - 建议提前购买FastPass节省排队时间
            - 记得带好防晒用品和舒适的鞋子
            - 烟花表演是每天的固定节目，不容错过！
            """,
            context.getVisitDate(),
            context.getVisitorCount(),
            context.getVisitorCount(), context.getVisitorCount() * 599,
            context.getVisitorCount(), context.getVisitorCount() * 200,
            context.getVisitorCount() * 799
        );
    }*/
}

