package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 路由节点
 * 用于判断工作流是首次执行还是从暂停状态恢复
 * 如果是恢复执行，直接路由到暂停的节点继续执行
 */
@Slf4j
public class ResumeRouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 恢复路由节点");
            log.info("是否为恢复模式: {}, 暂停节点: {}", 
                context.isResuming(), context.getPausedAtNode());

            // 如果是恢复模式，设置当前步骤信息
            if (context.isResuming()) {
                context.setCurrentStep("从暂停节点恢复: " + context.getPausedAtNode());
                log.info("工作流将从 {} 节点恢复执行", context.getPausedAtNode());
            } else {
                context.setCurrentStep("首次执行工作流");
                log.info("工作流首次执行，从 create_plan 开始");
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}

