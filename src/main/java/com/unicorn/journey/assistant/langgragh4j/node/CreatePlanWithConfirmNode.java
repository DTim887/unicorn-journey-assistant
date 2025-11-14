package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建行程节点（需要等待确认）
 * 该节点会创建行程并等待用户确认
 */
@Slf4j
public class CreatePlanWithConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 创建行程（等待确认）, sessionId={}", context.getSessionId());

            // 模拟创建行程
            Plan plan = createMockPlan(context);
            String planId = String.valueOf(plan.getId());
            context.setPlan(plan);
            context.setPlanId(planId);
            context.setCurrentStep("wait_confirm_plan");
            // 设置为未确认状态
            context.setPlanConfirmed(false);
            
            log.info("行程创建完成，等待用户确认: planId={}", planId);

            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 模拟创建行程
     */
    private static Plan createMockPlan(WorkflowContext context) {
        Plan plan = new Plan();
        plan.setId(System.currentTimeMillis()); // 使用时间戳作为ID
        plan.setUserId(context.getUser() != null ? context.getUser().getId() : 1);
        plan.setPlanName("上海迪士尼一日游");
        plan.setPlanDate(java.time.LocalDate.now().plusDays(7));
        
        // 添加景点
        List<Plan.PlanAttractionItem> attractions = new ArrayList<>();
        Plan.PlanAttractionItem item1 = new Plan.PlanAttractionItem();
        item1.setAttractionId(1);
        item1.setVisitTimeRange("09:00-11:00");
        attractions.add(item1);
        
        Plan.PlanAttractionItem item2 = new Plan.PlanAttractionItem();
        item2.setAttractionId(2);
        item2.setVisitTimeRange("11:00-13:00");
        attractions.add(item2);
        
        plan.setAttractionIds(attractions);
        
        return plan;
    }

    /**
     * 获取行程摘要信息（用于流式输出）
     */
    public static String getPlanSummary(Plan plan) {
        if (plan == null) {
            return "行程信息为空";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(" **行程信息**\n\n");
        summary.append("- **行程ID**: ").append(plan.getId()).append("\n");
        summary.append("- **行程名称**: ").append(plan.getPlanName()).append("\n");
        summary.append("- **游玩日期**: ").append(plan.getPlanDate()).append("\n");
        summary.append("- **用户ID**: ").append(plan.getUserId()).append("\n\n");
        
        if (plan.getAttractionIds() != null && !plan.getAttractionIds().isEmpty()) {
            summary.append("**景点列表**:\n");
            for (int i = 0; i < plan.getAttractionIds().size(); i++) {
                Plan.PlanAttractionItem item = plan.getAttractionIds().get(i);
                summary.append((i + 1)).append(". 景点ID: ").append(item.getAttractionId())
                       .append(" (时间: ").append(item.getVisitTimeRange()).append(")\n");
            }
        }
        
        summary.append("\n⏳ **等待您的确认...**");
        
        return summary.toString();
    }
}

