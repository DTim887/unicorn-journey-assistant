package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.checkpoint.CheckpointStorage;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 完成节点
 * 工作流执行完成后的最终节点
 */
@Slf4j
public class CompletionNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 工作流完成, sessionId={}", context.getSessionId());

            // 清理 checkpoint
            CheckpointStorage checkpointStorage = SpringBeanUtils.getBean(CheckpointStorage.class);
            checkpointStorage.removeCheckpoint(context.getSessionId());
            
            // 更新状态
            context.setCurrentStep("final");
            
            // 更新订单状态
            if (context.getOrder() != null) {
                context.getOrder().setStatus("已确认");
            }
            
            log.info("工作流执行完成: sessionId={}, planId={}, orderId={}", 
                context.getSessionId(), context.getPlanId(), context.getOrderId());

            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 获取完成摘要信息（用于流式输出）
     */
    public static String getCompletionSummary(WorkflowContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("✅ **工作流执行完成**\n\n");
        summary.append("恭喜！您的行程和订单已成功创建：\n\n");
        summary.append("📋 **行程ID**: ").append(context.getPlanId()).append("\n");
        summary.append("🎫 **订单ID**: ").append(context.getOrderId()).append("\n\n");
        summary.append("感谢您的使用！祝您旅途愉快！🎉");
        
        return summary.toString();
    }
}

