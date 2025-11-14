package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 菜单确认节点
 */
@Slf4j
@RequiredArgsConstructor
public class ConfirmMenuNode {

    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建确认节点
     */
    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 菜单确认节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null) {
                return HotelAssistantContext.saveContext(context);
            }

            String confirmationResult = context.getConfirmationResult();
            log.info("用户确认结果: {}", confirmationResult);

            // 如果确认结果为空，说明菜单已生成但还未确认，需要暂停等待用户确认
            if (StringUtils.isBlank(confirmationResult)) {
                log.info("菜单已生成，等待用户确认");
                
                // 标记需要用户确认
                context.setNeedConfirmation(true);
                context.setConfirmationType("MENU");
                context.setPausedAtNode("confirm_menu");
                context.setCurrentStep("请确认菜单是否满意");
                
                // 发送确认请求事件
                HotelSseHelper.sendSseEvent(
                    context.getSessionId(),
                    checkpointService,
                    HotelSSEEventType.CONFIRMATION_REQUIRED,
                    Map.of(
                        "confirmationType", "MENU",
                        "message", "请确认菜单是否满意，您可以选择确认、重新生成或拒绝"
                    )
                );
                
                return HotelAssistantContext.saveContext(context);
            }

            if ("approved".equalsIgnoreCase(confirmationResult)) {
                // 用户确认满意，标记任务完成
                currentTask.setStatus("COMPLETED");
                context.setCurrentStep("菜单已确认，任务完成");
                log.info("用户确认菜单满意，任务完成");
            } else if ("regenerate".equalsIgnoreCase(confirmationResult)) {
                // 用户要求重新生成，清除当前菜单结果，重置状态
                currentTask.setStatus("PENDING");
                currentTask.setResult(null);
                context.setCurrentStep("重新生成菜单");
                log.info("用户要求重新生成菜单，将返回生成节点");
                // 注意：不在这里清除 confirmationResult，让路由函数先判断
            } else if ("rejected".equalsIgnoreCase(confirmationResult)) {
                // 用户拒绝
                currentTask.setStatus("REJECTED");
                context.setCurrentStep("用户拒绝了菜单");
                log.info("用户拒绝菜单");
            } else {
                // 未知的确认结果
                log.warn("未知的确认结果: {}", confirmationResult);
                context.setCurrentStep("确认结果异常，请联系工作人员");
            }

            // 清除确认标志（confirmationResult 在路由后清除）
            context.setNeedConfirmation(false);

            return HotelAssistantContext.saveContext(context);
        });
    }
}
