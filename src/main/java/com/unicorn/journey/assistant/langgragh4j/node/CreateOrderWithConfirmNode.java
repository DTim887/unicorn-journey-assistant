package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.agent.WorkflowOrderAgent;
import com.unicorn.journey.assistant.langgragh4j.enums.SSEEventTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建订单节点
 * 该节点专门负责创建订单，不包含确认逻辑
 */
@Slf4j
public class CreateOrderWithConfirmNode {

    private final WorkflowAgentFactory agentFactory;
    private final OrderService orderService;
    private final WorkflowCheckpointService checkpointService;

    public CreateOrderWithConfirmNode(WorkflowAgentFactory agentFactory, OrderService orderService, WorkflowCheckpointService checkpointService) {
        this.agentFactory = agentFactory;
        this.orderService = orderService;
        this.checkpointService = checkpointService;
    }

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 创建订单 - AI生成");

            // 检查是否有行程ID
            if (context.getPlanId() == null) {
                log.warn("没有行程ID，无法创建订单");
                context.setCurrentStep("错误：缺少行程信息，无法创建订单");
                return ConfirmWorkflowContext.saveContext(context);
            }

            // 检查用户是否已有 order
            if (context.getUser() != null) {
                List<Order> existingOrders = orderService.retrieveOrdersByUserId(context.getUser().getId());
                if (existingOrders != null && !existingOrders.isEmpty()) {
                    log.info("用户已存在 {} 个订单, userId={}", existingOrders.size(), context.getUser().getId());
                    // 使用最新的订单
                    Order latestOrder = existingOrders.getLast();
                    context.setOrder(latestOrder);
                    context.setOrderId(latestOrder.getId());
                    context.setCurrentStep("检测到用户已有订单，将使用现有订单");
                    return ConfirmWorkflowContext.saveContext(context);
                }
            }

            // 生成订单ID
            String orderId = "ORDER-" + UUID.fastUUID().toString().substring(0, 8).toUpperCase();
            context.setOrderId(orderId);

            try {
                // 使用 AI Agent 生成订单
                WorkflowOrderAgent orderAgent = agentFactory.getOrderAgent(context.getSessionId());

                log.info("调用 AI 生成订单: planId={}, visitDate={}, visitorCount={}",
                        context.getPlanId(), context.getVisitDate(), context.getVisitorCount());

                // 直接调用非流式 API 获取完整内容
                String orderContent = orderAgent.createOrder(
                        context.getUser(),
                        context.getPlanId(),
                        context.getVisitDate(),
                        context.getVisitorCount(),
                        "请根据plan和游玩信息生成订单"
                );
                
                log.info("AI 订单生成完成，内容长度: {}", orderContent.length());
                
                // 发送 SSE 事件给前端
                SseEmitter emitter = checkpointService.getEmitter(context.getSessionId());
                String sessionId = context.getSessionId();
                if (emitter != null) {
                    try {
                        String jsonData = JSONUtil.toJsonStr(Map.of(
                                "sseEventType", SSEEventTypeEnum.OUTPUT_CHUNK.getCode(),
                                "content", orderContent,
                                "nodeName", "create_order",
                                "confirmationType", "ORDER",
                                "planId", context.getPlanId() != null ? context.getPlanId() : "",
                                "orderId", orderId
                        ));
                        emitter.send(SseEmitter.event()
                                .name(SSEEventTypeEnum.OUTPUT_CHUNK.getCode())
                                .data(jsonData));
                        log.info("发送订单生成结果 SSE 事件: sessionId={}", sessionId);
                    } catch (Exception e) {
                        log.error("发送 SSE 事件失败: sessionId={}", sessionId, e);
                    }
                }
                
                // 保存 AI 生成的订单内容
                context.setCurrentStep("创建订单完成\n\n订单ID: " + orderId + "\n\n" + orderContent);

            } catch (Exception e) {
                log.error("AI 生成订单失败", e);
                context.setCurrentStep("创建订单失败");
            }

            log.info("订单创建完成，订单ID: {}, 关联行程ID: {}", orderId, context.getPlanId());

            return ConfirmWorkflowContext.saveContext(context);
        });
    }

    /**
     * 生成默认订单模板（AI 失败时的备选方案）
     */
    /*private String generateDefaultOrder(ConfirmWorkflowContext context, String orderId) {
        int visitorCount = context.getVisitorCount() != null ? context.getVisitorCount() : 1;
        int ticketPrice = visitorCount * 599;
        int fastPassPrice = visitorCount * 150;
        int mealPrice = visitorCount * 200;
        int servicePrice = (int) ((ticketPrice + fastPassPrice + mealPrice) * 0.03);
        int totalPrice = ticketPrice + fastPassPrice + mealPrice + servicePrice;

        return String.format("""
            📝 订单详情
            
            订单号：%s
            关联行程：%s
            创建时间：%s
            
            📦 订单内容：
            - 上海迪士尼乐园门票 × %d人
            - FastPass 快速通行证 × %d人
            - 餐饮券套餐 × %d人
            
            💰 费用明细：
            - 门票费用：¥%d
            - FastPass：¥%d
            - 餐饮券：¥%d
            - 服务费：¥%d
            ━━━━━━━━━━━━━━
            总计：¥%d
            
            📅 使用信息：
            - 使用日期：%s
            - 游客人数：%d人
            
            📌 注意事项：
            - 请在游玩日期当天凭订单号入园
            - FastPass 可在指定时间段快速通行
            - 餐饮券可在指定餐厅使用
            """,
            orderId,
            context.getPlanId(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            visitorCount, visitorCount, visitorCount,
            ticketPrice, fastPassPrice, mealPrice, servicePrice, totalPrice,
            context.getVisitDate(),
            visitorCount
        );
    }*/
}

