package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.checkpoint.CheckpointStorage;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 等待订单确认节点
 * 该节点会保存 checkpoint 并等待用户确认
 * 用户确认后才会继续执行下一个节点
 */
@Slf4j
public class WaitForOrderConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 等待订单确认, sessionId={}", context.getSessionId());

            // 保存 checkpoint
            CheckpointStorage checkpointStorage = SpringBeanUtils.getBean(CheckpointStorage.class);
            checkpointStorage.saveCheckpoint(context.getSessionId(), context, "WAIT_ORDER_CONFIRM");
            
            // 设置 checkpointId
            context.setCheckpointId(context.getSessionId() + "_WAIT_ORDER_CONFIRM");
            
            log.info("Checkpoint 已保存，等待用户确认订单: sessionId={}", context.getSessionId());

            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 判断是否应该继续执行（用于条件边）
     */
    public static String shouldContinue(WorkflowContext context) {
        if (Boolean.TRUE.equals(context.getOrderConfirmed())) {
            log.info("订单已确认，继续执行: sessionId={}", context.getSessionId());
            return "continue";
        } else {
            log.info("订单未确认，等待中: sessionId={}", context.getSessionId());
            return "wait";
        }
    }
}

