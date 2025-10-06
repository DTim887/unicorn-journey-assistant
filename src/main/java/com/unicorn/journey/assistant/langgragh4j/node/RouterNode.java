package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.ai.RouterService;
import com.unicorn.journey.assistant.langgragh4j.enums.BusinessTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由工作节点
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            BusinessTypeEnum businessType;
            try {
                // 获取AI路由服务
                RouterService routerService = SpringBeanUtils.getBean(RouterService.class);
                // 根据原始提示词进行智能路由
                businessType = routerService.routeBusinessType(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", businessType.getAssistant(), businessType.getDesc());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认CREATE_PLAN类型: {}", e.getMessage());
                businessType = BusinessTypeEnum.CREATE_PLAN;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setBusinessTypeEnum(businessType);
            return WorkflowContext.saveContext(context);
        });
    }
}