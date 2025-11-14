package com.unicorn.journey.assistant.langgragh4j.state;

import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.langgragh4j.enums.BusinessTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowContext implements Serializable {
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
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 用户 id
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
     * 业务类型
     */
    private BusinessTypeEnum businessTypeEnum;

    /**
     * 行程ID（创建后的ID）
     */
    private String planId;

    /**
     * 订单ID（创建后的ID）
     */
    private String orderId;

    /**
     * 行程是否已确认
     */
    private Boolean planConfirmed;

    /**
     * 订单是否已确认
     */
    private Boolean orderConfirmed;

    /**
     * checkpoint ID - 用于恢复工作流
     */
    private String checkpointId;

    /**
     * 会话ID - 用于隔离不同用户的交互
     */
    private String sessionId;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}
