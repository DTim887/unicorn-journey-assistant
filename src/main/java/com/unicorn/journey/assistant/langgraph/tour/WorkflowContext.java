package com.unicorn.journey.assistant.langgraph.tour;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bsc.langgraph4j.state.AgentState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
public class WorkflowContext implements Serializable {

    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    //审批结果常量
    public static final String APPROVED = "同意";
    public static final String REJECTED = "拒绝";

    //工作流ID
    private String workflowId;
    //当前节点
    private String event;
    //当前状态
    private String currentStep;
    //是否需要审批
    private Boolean needApproval;
    //审批结果
    private String approvalResult;
    //AiMessage
    private List<String> createPlanUserMessages = new ArrayList<>();



    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(AgentState state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }

}
