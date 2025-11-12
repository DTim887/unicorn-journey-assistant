package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.Snowflake;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.time.LocalDate;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 行程确认节点
 * 该节点专门负责处理行程确认逻辑，会在需要确认时暂停工作流
 * 确认通过后保存Plan到数据库
 */
@Slf4j
public class ConfirmPlanNode {

    private final PlanService planService;
    private final Snowflake snowflake;

    public ConfirmPlanNode(PlanService planService) {
        this.planService = planService;
        this.snowflake = new Snowflake();
    }

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);
            log.info("执行节点: 行程确认");

            // 检查是否已经有确认结果
            if (StringUtils.isBlank(context.getConfirmationResult())) {
                // 如果没有确认结果，设置需要确认
                log.info("需要用户确认行程，设置暂停标志");
                context.setCurrentStep("等待行程确认");
                context.setNeedConfirmation(true);
                context.setConfirmationType("PLAN");

                // 记录暂停位置 - 恢复时应该从 confirm_plan 的下一个节点开始
                context.setPausedAtNode("confirm_plan");
                log.info("记录暂停位置: {}", "confirm_plan");
            } else {
                // 如果已经有确认结果
                String result = context.getConfirmationResult();
                log.info("用户已确认行程: {}", result);
                
                // 只有在确认通过时才真正保存Plan
                if ("approved".equalsIgnoreCase(result)) {
                    log.info("用户确认通过，检查Plan是否已保存");
                    
                    // 检查用户是否已有 plan
                    Plan existingPlan = planService.retrievePlanByUserId(context.getUser().getId());
                    if (existingPlan == null) {
                        log.info("Plan已存在，使用现有Plan: planId={}, userId={}", "1", context.getUser().getId());
                        context.setPlan(new Plan());// TODO
                        context.setPlanId("PLAN-1");
                        //context.setPlanId("PLAN-1" + existingPlan.getId());

                        context.setCurrentStep("行程确认完成，已使用现有行程");
                    } else {
                        log.info("Plan不存在，等待重新生成");
                        context.setPlan(null);
                        context.setPlanId(null);
                        context.setCurrentStep("生成plan失败，等待重新生成");
                    }
                } else if ("rejected".equalsIgnoreCase(result) || "regenerate".equalsIgnoreCase(result)) {
                    // 用户拒绝或要求重新生成，删除已生成的 plan
                    if (context.getUser() != null) {
                        Plan existingPlan = planService.retrievePlanByUserId(context.getUser().getId());
                        if (existingPlan != null) {
                            log.info("删除已生成的Plan: planId={}, userId={}", existingPlan.getId(), context.getUser().getId());
                            planService.evict(context.getUser().getId());
                            context.setPlan(null);
                            context.setPlanId(null);
                        }
                    }
                    
                    if ("rejected".equalsIgnoreCase(result)) {
                        log.info("用户拒绝行程");
                        context.setCurrentStep("行程已拒绝");
                    } else {
                        log.info("用户要求重新生成行程");
                        context.setCurrentStep("准备重新生成行程");
                    }
                }
                
                context.setNeedConfirmation(false);
                context.setConfirmationType(null);
                context.setConfirmationResult(null);
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}


