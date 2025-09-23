package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.constant.OrderStatus;
import com.unicorn.journey.assistant.constant.RefundType;
import com.unicorn.journey.assistant.controller.request.RefundOrderRequest;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Refund;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@LocalCache(value = CacheName.REFUND)
public class RefundService extends BaseService<Refund> {

    private final OrderService orderService;

    public RefundService(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * @param refundOrderRequest
     */
    @Tool("Tool: 通过订单ID和退款金额对订单进行退款")
    public void refundOrder(RefundOrderRequest refundOrderRequest) {
        //保存订单状态为已退款
        Order order = orderService.get(refundOrderRequest.getOrderId());
        order.setStatus(OrderStatus.REFUND.name());
        orderService.saveOrder(order);
        //记录Refund日志
        Refund refund = new Refund();
        refund.setOrderId(refundOrderRequest.getOrderId());
        refund.setUserId(refundOrderRequest.getUserId());
        refund.setRefundTime(LocalDateTime.now().toString());
        refund.setTotalPrice(order.getTotalPrice());
        refund.setRefundPrice(refundOrderRequest.getRefundPrice());
        refund.setType(order.getTotalPrice() == refundOrderRequest.getRefundPrice() ? RefundType.ALL.name() : RefundType.PARTIAL.name());
        this.put(refund.getOrderId(), refund);
    }

    public List<Refund> retrieveRefundsByUserId(int userId) {
        List<Refund> refunds = this.getAll(Refund.class);
        if (!CollectionUtils.isEmpty(refunds)) {
            return refunds.stream()
                    .filter(order -> userId == order.getUserId())
                    .collect(Collectors.toList());
        }
        return null;
    }
}
