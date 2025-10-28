package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 行程确认节点
 * 该节点专门负责处理行程确认逻辑，会在需要确认时暂停工作流
 */
@Slf4j
public class ConfirmPlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
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
                // 如果已经有确认结果，重置确认标志
                log.info("用户已确认行程: {}", context.getConfirmationResult());
                context.setNeedConfirmation(false);
                context.setConfirmationType(null);
                context.setConfirmationResult(null);
                context.setCurrentStep("行程确认完成");
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}


