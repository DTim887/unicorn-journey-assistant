package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.enums.ConfirmTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 工作流完成节点
 * 处理工作流的最终状态
 */
@Slf4j
public class CompletionNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 工作流完成");

            // 根据确认状态设置完成信息
            if (ConfirmTypeEnum.REJECTED.getCode().equalsIgnoreCase(context.getConfirmationResult())) {
                context.setCurrentStep("工作流已取消");
                log.info("工作流已取消 - 用户拒绝确认");
            } else {
                context.setCurrentStep("工作流成功完成");
                log.info("工作流成功完成 - 行程ID: {}, 订单ID: {}",
                        context.getPlanId(), context.getOrderId());
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}

