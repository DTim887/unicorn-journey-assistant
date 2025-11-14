package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.langgragh4j.checkpoint.CheckpointStorage;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 等待行程确认节点
 * 用户确认后才会继续执行下一个节点
 */
@Slf4j
public class WaitForPlanConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 等待行程确认, sessionId={}", context.getSessionId());

            // 保存 checkpoint
            CheckpointStorage checkpointStorage = SpringBeanUtils.getBean(CheckpointStorage.class);
            checkpointStorage.saveCheckpoint(context.getSessionId(), context, "WAIT_PLAN_CONFIRM");
            
            // 设置 checkpointId
            context.setCheckpointId(context.getSessionId() + "_WAIT_PLAN_CONFIRM");
            
            log.info("Checkpoint 已保存，等待用户确认行程: sessionId={}", context.getSessionId());

            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 判断是否应该继续执行（用于条件边）
     */
    public static String shouldContinue(WorkflowContext context) {
        if (Boolean.TRUE.equals(context.getPlanConfirmed())) {
            log.info("行程已确认，继续执行: sessionId={}", context.getSessionId());
            return "continue";
        } else {
            log.info("行程未确认，等待中: sessionId={}", context.getSessionId());
            return "wait";
        }
    }
}

