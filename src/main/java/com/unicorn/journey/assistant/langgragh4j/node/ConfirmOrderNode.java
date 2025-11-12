package com.unicorn.journey.assistant.langgragh4j.node;

import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import com.unicorn.journey.assistant.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;
import java.util.UUID;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 订单确认节点
 * 该节点专门负责处理订单确认逻辑，会在需要确认时暂停工作流
 * 确认通过后保存Order到数据库
 */
@Slf4j
public class ConfirmOrderNode {

    private final OrderService orderService;

    public ConfirmOrderNode(OrderService orderService) {
        this.orderService = orderService;
    }

    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 订单确认");

            // 检查是否已经有确认结果
            if (StringUtils.isBlank(context.getConfirmationResult())) {
                // 如果没有确认结果，设置需要确认
                log.info("需要用户确认订单，设置暂停标志");
                context.setCurrentStep("等待订单确认");
                context.setNeedConfirmation(true);
                context.setConfirmationType("ORDER");
                // 记录暂停位置 - 恢复时应该从 confirm_order 的下一个节点开始
                context.setPausedAtNode("confirm_order");
                log.info("记录暂停位置: {}", "confirm_order");
            } else {
                // 如果已经有确认结果
                String result = context.getConfirmationResult();
                log.info("用户已确认订单: {}", result);
                
                // 只有在确认通过时才真正保存Order
                if ("approved".equalsIgnoreCase(result)) {
                    log.info("用户确认通过，检查Order是否已保存");
                    
                    // 检查用户是否已有 order
                    List<Order> existingOrders = orderService.retrieveOrdersByUserId(context.getUser().getId());
                    if (existingOrders == null || existingOrders.isEmpty()) {
                        log.info("Order不存在，等待重新生成");
                        context.setOrder(new Order());// TODO
                        context.setOrderId("111");
                        context.setCurrentStep("生成订单失败，等待重新生成");
                    } else {
                        log.info("Order存在，保存到上下文中");
                        Order order = existingOrders.getLast();
                        // 保存到上下文
                        context.setOrder(order);
                        context.setOrderId("ORDER-" + order.getId());
                        context.setCurrentStep("订单确认完成");
                        log.info("Order已确认: orderId={}, userId={}", order.getId(), order.getUserId());
                    }
                } else if ("rejected".equalsIgnoreCase(result) || "regenerate".equalsIgnoreCase(result)) {
                    // 用户拒绝或要求重新生成，删除已生成的 order
                    if (context.getUser() != null) {
                        List<Order> existingOrders = orderService.retrieveOrdersByUserId(context.getUser().getId());
                        if (existingOrders != null && !existingOrders.isEmpty()) {
                            log.info("删除已生成的Order: orderCount={}, userId={}", existingOrders.size(), context.getUser().getId());
                            // 删除所有订单
                            for (Order order : existingOrders) {
                                orderService.evict(order.getId());
                            }
                            context.setOrder(null);
                            context.setOrderId(null);
                        }
                    }
                    
                    if ("rejected".equalsIgnoreCase(result)) {
                        log.info("用户拒绝订单");
                        context.setCurrentStep("订单已拒绝");
                    } else {
                        log.info("用户要求重新生成订单");
                        context.setCurrentStep("准备重新生成订单");
                    }
                }
                
                context.setNeedConfirmation(false);
                context.setConfirmationType(null);
                context.setConfirmationResult(null);
            }

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}


