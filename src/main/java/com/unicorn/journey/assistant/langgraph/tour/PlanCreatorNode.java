package com.unicorn.journey.assistant.langgraph.tour;

import com.unicorn.journey.assistant.agent.PlanCreatorAgent;
import com.unicorn.journey.assistant.chat.AiAgentFactory;
import com.unicorn.journey.assistant.chat.CreatePlanResponse;
import com.unicorn.journey.assistant.constant.Agents;
import com.unicorn.journey.assistant.utils.SpringBeanUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class PlanCreatorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext workflowContext = WorkflowContext.getContext(state);
            AiAgentFactory aiAgentFactory = SpringBeanUtils.getBean(AiAgentFactory.class);
            PlanCreatorAgent planCreatorAgent = (PlanCreatorAgent) aiAgentFactory.getAiAgent(Agents.PLAN_CREATOR, workflowContext.getWorkflowId());
            CreatePlanResponse aiResponse = planCreatorAgent.plan(workflowContext.getWorkflowId(), workflowContext.getCreatePlanUserMessages().getLast());
            if(aiResponse!=null && aiResponse.getNeedApproval()){
                //需要审批才能调用工具创建行程, 初始化审批结果
                workflowContext.setNeedApproval(true);
            }
            workflowContext.setCurrentStep(PlannerApp.PLAN_CREATOR_NODE);
            workflowContext.getCreatePlanUserMessages().add(aiResponse.getAnswer());
            //保存上下文
            return WorkflowContext.saveContext(workflowContext);
        });
    }

}
