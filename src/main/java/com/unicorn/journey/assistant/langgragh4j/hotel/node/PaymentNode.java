package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.PaymentAgent;
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
 * 支付节点
 */
@Slf4j
@RequiredArgsConstructor
public class PaymentNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建支付节点
     */
    public AsyncNodeAction<org.bsc.langgraph4j.prebuilt.MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 支付节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null || !"ROOM_RENEWAL".equals(currentTask.getTaskType())) {
                log.warn("当前任务不是续约任务");
                return HotelAssistantContext.saveContext(context);
            }

            try {
                // 从任务参数中获取订单信息
                Map<String, Object> params = currentTask.getParams();
                String orderId = (String) params.get("orderId");
                
                // 模拟从订单内容中提取金额
                String amount = calculateAmount(params);

                log.info("开始处理支付: orderId={}, amount={}", orderId, amount);

                // 调用 AI Agent 处理支付（Mock）
                PaymentAgent agent = agentFactory.getPaymentAgent(context.getSessionId());
                String paymentResult = agent.processPayment(
                        orderId,
                        amount,
                        "请处理支付"
                );

                log.info("支付处理完成，内容长度: {}", paymentResult.length());

                // 发送 SSE 事件展示支付结果
                HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, paymentResult, "payment");

                // 更新任务结果
                String finalResult = currentTask.getResult() + "\n\n" + paymentResult;
                currentTask.setResult(finalResult);
                context.getTaskResults().put(currentTask.getTaskId(), finalResult);
                currentTask.setStatus("COMPLETED");
                context.setCurrentStep("续约订单支付完成");

                log.info("支付完成: orderId={}", orderId);

            } catch (Exception e) {
                log.error("支付失败", e);
                currentTask.setStatus("FAILED");
                currentTask.setResult(currentTask.getResult() + "\n\n支付失败: " + e.getMessage());
                context.setCurrentStep("支付失败");
            }

            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 计算金额（简化处理）
     */
    private String calculateAmount(Map<String, Object> params) {
        // 这里简化处理，实际应该从 AI 生成的订单内容中解析
        Integer renewalDays = params.get("renewalDays") != null ?
                Integer.parseInt(params.get("renewalDays").toString()) : 1;
        String roomType = (String) params.getOrDefault("roomType", "标准间");
        
        int pricePerNight = switch (roomType) {
            case "豪华间" -> 499;
            case "豪华套房" -> 899;
            case "总统套房" -> 1999;
            default -> 299; // 标准间
        };
        
        double totalAmount = pricePerNight * renewalDays;
        
        // 续约优惠
        if (renewalDays >= 7) {
            totalAmount *= 0.9;
        } else if (renewalDays >= 3) {
            totalAmount *= 0.95;
        }
        
        return String.format("%.2f", totalAmount);
    }
}
