package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建行程节点
 * 该节点专门负责创建行程，不包含确认逻辑
 */
@Slf4j
public class CreatePlanWithConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 创建行程");

            // 生成行程ID
            String planId = "PLAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            context.setPlanId(planId);

            // TODO: 创建行程
            // Mock 行程数据 - 模拟 AI 生成的行程信息
            StringBuilder planDetails = new StringBuilder();
            planDetails.append("上海迪士尼乐园一日游行程\n\n");
            planDetails.append("日期：2025年10月23日（周四）\n");
            planDetails.append("时间：09:00 - 21:00\n\n");
            planDetails.append("推荐路线：\n");
            planDetails.append("1. 09:00 - 入园，前往探险岛\n");
            planDetails.append("2. 09:30 - 飞跃地平线（FastPass）\n");
            planDetails.append("3. 10:30 - 雷鸣山漂流\n");
            planDetails.append("4. 12:00 - 午餐：巴波萨烧烤\n");
            planDetails.append("5. 14:00 - 宝藏湾-加勒比海盗\n");
            planDetails.append("6. 15:30 - 梦幻世界-七个小矮人矿山车\n");
            planDetails.append("7. 17:00 - 晚餐：皇家宴会厅\n");
            planDetails.append("8. 19:30 - 观看烟花表演\n\n");
            planDetails.append("💰 预估费用：\n");
            planDetails.append("- 门票：¥599/人\n");
            planDetails.append("- 餐饮：¥200/人\n");
            planDetails.append("- 总计：¥799/人\n");

            context.setCurrentStep("创建行程" + planDetails);

            log.info("行程创建完成，行程ID: {}", planId);

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}

