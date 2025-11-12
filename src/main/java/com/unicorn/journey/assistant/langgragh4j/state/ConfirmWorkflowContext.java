package com.unicorn.journey.assistant.langgragh4j.state;

import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmWorkflowContext implements Serializable {
    @Serial
    private static final long serialVersionUID = 123L;

    /**
     * WorkflowContext 在 MessagesState 中的存储key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 用户
     */
    private User user;

    /**
     * plan
     */
    private Plan plan;

    /**
     * 订单
     */
    private Order order;

    /**
     * 是否需要用户确认
     */
    private boolean needConfirmation;

    /**
     * 确认类型 (PLAN / ORDER)
     */
    private String confirmationType;

    /**
     * 用户确认结果 (approved / rejected / regenerate)
     */
    private String confirmationResult;

    /**
     * 行程ID
     */
    private String planId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 暂停时所在的节点名称 - 用于恢复时直接跳转
     */
    private String pausedAtNode;

    /**
     * 是否为恢复执行模式
     */
    private boolean isResuming;

    /**
     * 游玩日期
     */
    private String visitDate;

    /**
     * 游玩人数
     */
    private Integer visitorCount;

    /**
     * 用户输入的消息内容（用于AI处理）
     */
    private String userMessage;

    /**
     * SSE发射器（非序列化字段，用于节点内实时发送事件）
     */
    private transient SseEmitter sseEmitter;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static ConfirmWorkflowContext getContext(MessagesState<String> state) {
        return (ConfirmWorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(ConfirmWorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}
