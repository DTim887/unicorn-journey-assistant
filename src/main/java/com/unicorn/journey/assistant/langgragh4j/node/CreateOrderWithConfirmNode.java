package com.unicorn.journey.assistant.langgragh4j.node;

import cn.hutool.core.lang.UUID;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 创建订单节点
 * 该节点专门负责创建订单，不包含确认逻辑
 */
@Slf4j
public class CreateOrderWithConfirmNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(state);

            log.info("执行节点: 创建订单");

            // 检查是否有行程ID
            if (context.getPlanId() == null) {
                log.warn("没有行程ID，无法创建订单");
            }

            // 生成订单ID
            String orderId = "ORDER-" + UUID.fastUUID().toString().substring(0, 8).toUpperCase();
            context.setOrderId(orderId);

            // TODO: 创建订单
            // Mock 订单数据
            StringBuilder orderDetails = new StringBuilder();
            orderDetails.append("订单详情\n\n");
            orderDetails.append("订单号：").append(orderId).append("\n");
            orderDetails.append("关联行程：").append(context.getPlanId()).append("\n\n");
            orderDetails.append("订单内容：\n");
            orderDetails.append("上海迪士尼乐园门票 x2\n");
            orderDetails.append("FastPass 快速通行证 x2\n");
            orderDetails.append("餐饮券套餐 x2\n\n");
            orderDetails.append("费用明细：\n");
            orderDetails.append("门票费用：¥1,198\n");
            orderDetails.append("FastPass：¥300\n");
            orderDetails.append("餐饮券：¥400\n");
            orderDetails.append("服务费：¥50\n");
            orderDetails.append("总计：¥1,948\n\n");
            orderDetails.append("使用日期：2025年10月23日\n");
            orderDetails.append("游客人数：2人\n");

            context.setCurrentStep("创建订单完成" + orderDetails);

            log.info("订单创建完成，订单ID: {}, 关联行程ID: {}", orderId, context.getPlanId());

            return ConfirmWorkflowContext.saveContext(context);
        });
    }
}

