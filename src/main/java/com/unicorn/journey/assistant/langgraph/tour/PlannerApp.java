package com.unicorn.journey.assistant.langgraph.tour;

import com.unicorn.journey.assistant.controller.request.ApproveWorkflowDTO;
import com.unicorn.journey.assistant.controller.request.MagicDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class PlannerApp {

    // 用于存储与客户端的 SSE 连接，Key 是 workflowId
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    // 用于存储暂停的工作流状态，Key 是 workflowId
    private static final Map<String, MessagesState<String>> pausedStates = new ConcurrentHashMap<>();


    public static String PLAN_CREATOR_NODE = "PLAN_CREATOR_NODE";
    public static String ORDER_CREATOR_NODE = "ORDER_CREATOR_NODE";

    public void startWorkflow(SseEmitter sseEmitter, MagicDTO magicDTO) {
//        String workflowId = UUID.randomUUID().toString();
        String workflowId = "1";
        emitters.put(workflowId, sseEmitter);
        Thread.startVirtualThread(() -> {
            try {
                MessagesState<String> lastState = null;
                WorkflowContext initialContext = WorkflowContext.builder()
                        .workflowId(workflowId)
                        .currentStep(PLAN_CREATOR_NODE)
                        .createPlanUserMessages(new ArrayList<>(List.of("上海天气怎么样")))
                        .build();
                StateGraph<MessagesState<String>> graphDefinition = this.getGraph();

                CompiledGraph<MessagesState<String>> graph = graphDefinition.compile();
                GraphRepresentation graphGraph = graph.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("工作流图：{}", graphGraph.getContent());
                for (NodeOutput<MessagesState<String>> item : graph.stream(WorkflowContext.saveContext(initialContext))) {
                    if (!(item.isSTART() || item.isEND())) {
                        lastState = item.state();
                        log.info("iterator : ********************");
                        sendSseEvent(sseEmitter, workflowId, item.node(), WorkflowContext.getContext(lastState));
                    }
                }
                assert lastState != null;
                WorkflowContext workflowContext = WorkflowContext.getContext(lastState);
                log.info("startWorkflow {}:{}", workflowContext.getNeedApproval(), workflowContext.getApprovalResult());
                //需要审批
                if (workflowContext.getNeedApproval()) {
                    // 且审批结果为空 -> 跳转到需要审批节点
                    if (StringUtils.isBlank(workflowContext.getApprovalResult())) {
                        log.info("status : ********************");
                        //暂存状态
                        pausedStates.put(workflowId, lastState);
                        sendSseEvent(sseEmitter, workflowId, "需要审批", WorkflowContext.getContext(lastState));
                    }
                } else {
                    // 工作流正常结束
                    sendSseEvent(sseEmitter, workflowId, "流程结束", WorkflowContext.getContext(lastState));
                    sseEmitter.complete();
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });

        sseEmitter.onCompletion(() -> emitters.remove(workflowId));
        sseEmitter.onTimeout(() -> {
            emitters.remove(workflowId);
            pausedStates.remove(workflowId);
        });
        sseEmitter.onError(e -> {
            emitters.remove(workflowId);
            pausedStates.remove(workflowId);
        });
    }

    public String approveWorkflow(ApproveWorkflowDTO approveWorkflowDTO) {
        MessagesState<String> pausedState = pausedStates.remove(approveWorkflowDTO.getWorkflowId());
        SseEmitter sseEmitter = emitters.get(approveWorkflowDTO.getWorkflowId());
        if (pausedState == null || sseEmitter == null) {
            return "Workflow not found or not in a paused state.";
        }
        Thread.startVirtualThread(() -> {
            try {
                MessagesState<String> lastState = null;
                StateGraph<MessagesState<String>> graphDefinition = this.getGraph();
                CompiledGraph<MessagesState<String>> graph = graphDefinition.compile();
                WorkflowContext workflowContext = WorkflowContext.getContext(pausedState);
                workflowContext.setApprovalResult(approveWorkflowDTO.getApprovalResult());
                workflowContext.getCreatePlanUserMessages().add(approveWorkflowDTO.getApprovalResult());
                for (NodeOutput<MessagesState<String>> item : graph.stream(WorkflowContext.saveContext(workflowContext))) {
                    if (!(item.isSTART() || item.isEND())) {
                        lastState = item.state();
                        sendSseEvent(sseEmitter, approveWorkflowDTO.getWorkflowId(), item.node(), WorkflowContext.getContext(lastState));
                    }
                    assert lastState != null;
                    WorkflowContext lastWorkflowContext = WorkflowContext.getContext(lastState);
                    log.info("approveWorkflow {}:{}", lastWorkflowContext.getNeedApproval(), lastWorkflowContext.getApprovalResult());

                    //需要审批
                    if (lastWorkflowContext.getNeedApproval()) {
                        // 且审批结果为空 -> 跳转到需要审批节点
                        if (StringUtils.isBlank(lastWorkflowContext.getApprovalResult())) {
                            //暂存状态
                            pausedStates.put(approveWorkflowDTO.getWorkflowId(), lastState);
                            sendSseEvent(sseEmitter, approveWorkflowDTO.getWorkflowId(), "需要审批", WorkflowContext.getContext(lastState));
                        }
                    } else {
                        // 工作流正常结束
                        sendSseEvent(sseEmitter, approveWorkflowDTO.getWorkflowId(), "流程结束", lastWorkflowContext);
                        sseEmitter.complete();
                    }
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });
        return "ok";
    }

    private String approveRouter(MessagesState<String> state) {
        WorkflowContext workflowContext = WorkflowContext.getContext(state);
        log.info("approveRouter {}:{}", workflowContext.getNeedApproval(), workflowContext.getApprovalResult());
        //需要审批
        if (workflowContext.getNeedApproval()) {
            //且审批通过
            if (WorkflowContext.APPROVED.equalsIgnoreCase(workflowContext.getApprovalResult())) {
                //重置审批状态
                workflowContext.setNeedApproval(false);
                workflowContext.setApprovalResult(null);
                log.info("--- approveRouter : approved ---");
                return "approved";
            } else if (WorkflowContext.REJECTED.equalsIgnoreCase(workflowContext.getApprovalResult())) {
                //重置审批状态
                workflowContext.setNeedApproval(false);
                workflowContext.setApprovalResult(null);
                return StateGraph.END;
            }
        }
        //重置审批状态
        workflowContext.setApprovalResult(null);
        WorkflowContext.saveContext(workflowContext);
        return StateGraph.END; // 特殊标识符，用于暂停/中断图的执行
    }

    private String router(MessagesState<String> state) {
        WorkflowContext workflowContext = WorkflowContext.getContext(state);
        return workflowContext.getCurrentStep();
    }

    private void sendSseEvent(SseEmitter emitter, String workflowId, String eventName, WorkflowContext workflowContext) throws IOException {
        workflowContext.setEvent(eventName);
        emitter.send(SseEmitter.event()
                .data(workflowContext));
//        SseEmitter.SseEventBuilder event = SseEmitter.event()
////                .id(workflowId)
////                .name(eventName)
//                .data(workflowContext);
//        emitter.send(event);
    }

    private StateGraph<MessagesState<String>> getGraph() throws GraphStateException {
        StateGraph<MessagesState<String>> graphDefinition = new MessagesStateGraph<>();
        graphDefinition.addNode(PLAN_CREATOR_NODE, PlanCreatorNode.create());
        graphDefinition.addNode(ORDER_CREATOR_NODE, OrderCreatorNode.create());
//        graphDefinition.addEdge(StateGraph.START,"PLAN_CREATOR_NODE");
//        graphDefinition.addEdge(PLAN_CREATOR_NODE,ORDER_CREATOR_NODE);
//        graphDefinition.addEdge(ORDER_CREATOR_NODE,StateGraph.END);

        graphDefinition.addConditionalEdges(StateGraph.START, edge_async(this::router), Map.of(PLAN_CREATOR_NODE, PLAN_CREATOR_NODE, ORDER_CREATOR_NODE, ORDER_CREATOR_NODE));
        graphDefinition.addConditionalEdges(PLAN_CREATOR_NODE, edge_async(this::approveRouter), Map.of("approved", ORDER_CREATOR_NODE, StateGraph.END, StateGraph.END));
        graphDefinition.addConditionalEdges(ORDER_CREATOR_NODE, edge_async(this::approveRouter), Map.of("approved", StateGraph.END, StateGraph.END, StateGraph.END));
        return graphDefinition;
    }
}
