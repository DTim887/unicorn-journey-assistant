package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import com.unicorn.journey.assistant.langgragh4j.hotel.agent.*;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 任务执行节点（处理非点餐类任务）
 */
@Slf4j
@RequiredArgsConstructor
public class TaskExecutorNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建任务执行节点
     */
    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 任务执行节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null) {
                log.warn("没有当前任务");
                return HotelAssistantContext.saveContext(context);
            }

            String taskType = currentTask.getTaskType();
            Map<String, Object> params = currentTask.getParams();

            try {
                String result = null;

                switch (taskType) {
                    case "ROOM_BOOKING":
                        result = executeRoomBooking(context.getSessionId(), params);
                        break;
                    case "WAKE_UP_CALL":
                        result = executeWakeUpCall(context.getSessionId(), params);
                        break;
                    case "CONCIERGE_SERVICE":
                        result = executeConciergeService(context.getSessionId(), params);
                        break;
                    default:
                        log.warn("未知的任务类型: {}", taskType);
                        result = "不支持的任务类型: " + taskType;
                        break;
                }

                if (result != null) {
                    // 发送 SSE 事件
                    HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, result, "task_executor");

                    // 保存任务结果
                    currentTask.setResult(result);
                    context.getTaskResults().put(currentTask.getTaskId(), result);
                    currentTask.setStatus("COMPLETED");
                    context.setCurrentStep("任务执行完成: " + currentTask.getDescription());
                }

            } catch (Exception e) {
                log.error("任务执行失败", e);
                currentTask.setStatus("FAILED");
                currentTask.setResult("任务执行失败: " + e.getMessage());
            }

            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 执行客房预订
     */
    private String executeRoomBooking(String sessionId, Map<String, Object> params) {
        RoomBookingAgent agent = agentFactory.getRoomBookingAgent(sessionId);
        String roomType = (String) params.getOrDefault("roomType", "标准间");
        String checkInDate = (String) params.get("checkInDate");
        Integer nights = params.get("nights") != null ? 
                Integer.parseInt(params.get("nights").toString()) : 1;

        log.info("执行客房预订: roomType={}, checkInDate={}, nights={}", roomType, checkInDate, nights);
        return agent.bookRoom(roomType, checkInDate, nights, "请预订客房");
    }

    /**
     * 执行叫醒服务
     */
    private String executeWakeUpCall(String sessionId, Map<String, Object> params) {
        WakeUpCallAgent agent = agentFactory.getWakeUpCallAgent(sessionId);
        String wakeUpTime = (String) params.get("wakeUpTime");

        log.info("执行叫醒服务: wakeUpTime={}", wakeUpTime);
        return agent.setWakeUpCall(wakeUpTime, "请设置叫醒服务");
    }

    /**
     * 执行礼宾服务
     */
    private String executeConciergeService(String sessionId, Map<String, Object> params) {
        ConciergeServiceAgent agent = agentFactory.getConciergeServiceAgent(sessionId);
        String serviceType = (String) params.get("serviceType");
        String details = (String) params.getOrDefault("details", "");

        log.info("执行礼宾服务: serviceType={}, details={}", serviceType, details);
        return agent.provideConciergeService(serviceType, details, "请提供礼宾服务");
    }
}
