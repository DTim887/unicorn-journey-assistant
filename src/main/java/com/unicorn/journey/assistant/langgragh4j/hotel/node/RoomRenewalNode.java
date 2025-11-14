package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.RoomRenewalAgent;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 续约订单生成节点
 */
@Slf4j
@RequiredArgsConstructor
public class RoomRenewalNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建续约订单节点
     */
    public AsyncNodeAction<org.bsc.langgraph4j.prebuilt.MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 续约订单生成节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null || !"ROOM_RENEWAL".equals(currentTask.getTaskType())) {
                log.warn("当前任务不是续约任务");
                return HotelAssistantContext.saveContext(context);
            }

            try {
                // 从任务参数中获取信息
                Map<String, Object> params = currentTask.getParams();
                String roomNumber = (String) params.getOrDefault("roomNumber", "未指定");
                Integer renewalDays = params.get("renewalDays") != null ?
                        Integer.parseInt(params.get("renewalDays").toString()) : 1;
                String roomType = (String) params.getOrDefault("roomType", "标准间");

                log.info("开始生成续约订单: roomNumber={}, renewalDays={}, roomType={}",
                        roomNumber, renewalDays, roomType);

                // 调用 AI Agent 生成续约订单
                RoomRenewalAgent agent = agentFactory.getRoomRenewalAgent(context.getSessionId());
                String renewalOrder = agent.generateRenewalOrder(
                        roomNumber,
                        renewalDays,
                        roomType,
                        "请生成续约订单"
                );

                log.info("续约订单生成完成，内容长度: {}", renewalOrder.length());

                // 发送 SSE 事件展示订单
                HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, renewalOrder, "room_renewal");

                // 从订单内容中提取订单号和金额（简化处理，实际应该解析 AI 返回的内容）
                String orderId = "RENEWAL-" + System.currentTimeMillis();
                
                // 保存订单号和订单内容到任务参数，供支付节点使用
                currentTask.getParams().put("orderId", orderId);
                currentTask.getParams().put("orderContent", renewalOrder);
                
                // 保存任务结果
                currentTask.setResult(renewalOrder);
                context.getTaskResults().put(currentTask.getTaskId(), renewalOrder);
                currentTask.setStatus("ORDER_GENERATED");
                context.setCurrentStep("续约订单已生成，准备支付");

                log.info("续约订单已生成: {}", orderId);

            } catch (Exception e) {
                log.error("生成续约订单失败", e);
                currentTask.setStatus("FAILED");
                currentTask.setResult("生成续约订单失败: " + e.getMessage());
                context.setCurrentStep("生成续约订单失败");
            }

            return HotelAssistantContext.saveContext(context);
        });
    }
}
