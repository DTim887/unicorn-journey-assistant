package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.langgragh4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建订单节点（支持确认）
 * 该节点会创建订单并等待用户确认
 */
@Slf4j
public class CreateOrderWithConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 创建订单（等待确认）, sessionId={}", context.getSessionId());

            // 确保行程已确认
            if (context.getPlan() == null || context.getPlanId() == null) {
                log.error("行程未创建，无法创建订单");
                throw new IllegalStateException("行程未创建，无法创建订单");
            }

            // 模拟创建订单
            Order order = createMockOrder(context);
            String orderId = order.getId();
            context.setOrder(order);
            context.setOrderId(orderId);
            context.setCurrentStep("wait_confirm_order");
            context.setOrderConfirmed(false); // 设置为未确认状态
            
            log.info("订单创建完成，等待用户确认: orderId={}", orderId);

            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 模拟创建订单
     */
    private static Order createMockOrder(WorkflowContext context) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(context.getUser() != null ? context.getUser().getId() : 999);
        order.setVisitDate(java.time.LocalDate.now().plusDays(7).toString());
        order.setStatus("待确认");
        order.setTotalPrice(1500);
        
        // 添加购买的产品
        List<Order.PurchasedProduct> products = new java.util.ArrayList<>();
        Order.PurchasedProduct product1 = new Order.PurchasedProduct();
        product1.setProductId(1);
        product1.setQuantity(2);
        products.add(product1);
        
        order.setPurchasedProducts(products);
        
        return order;
    }

    /**
     * 获取订单摘要信息（用于流式输出）
     */
    public static String getOrderSummary(Order order) {
        if (order == null) {
            return "订单信息为空";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("🎫 **订单信息**\n\n");
        summary.append("- **订单ID**: ").append(order.getId()).append("\n");
        summary.append("- **用户ID**: ").append(order.getUserId()).append("\n");
        summary.append("- **入园日期**: ").append(order.getVisitDate()).append("\n");
        summary.append("- **订单总价**: ¥").append(order.getTotalPrice()).append("\n");
        summary.append("- **状态**: ").append(order.getStatus()).append("\n\n");
        
        if (order.getPurchasedProducts() != null && !order.getPurchasedProducts().isEmpty()) {
            summary.append("**购买商品**:\n");
            for (int i = 0; i < order.getPurchasedProducts().size(); i++) {
                Order.PurchasedProduct product = order.getPurchasedProducts().get(i);
                summary.append((i + 1)).append(". 商品ID: ").append(product.getProductId())
                       .append(" (数量: ").append(product.getQuantity()).append(")\n");
            }
        }
        
        summary.append("\n⏳ **等待您的确认...**");
        
        return summary.toString();
    }
}

