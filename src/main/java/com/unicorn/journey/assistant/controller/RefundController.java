package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.RefundVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RefundController {

    private final OrderService orderService;

    @Autowired
    public RefundController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 根据订单ID获取退款日志
    @GetMapping("/refund/log/{orderId}")
    public Result getRefundLogByOrderId(@PathVariable String orderId) {
        try {
            List<RefundVO> refundLogs = orderService.getRefundLogsByOrderId(orderId);
            if (refundLogs.isEmpty()) {
                return Result.builder().code("1").msg("No refund log found for order ID: " + orderId).build();
            }
            // 返回第一个退款日志对象（假设一个订单只有一个退款记录）
            return Result.ok(refundLogs.get(0));
        } catch (IllegalArgumentException e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }

    // 获取所有退款日志
    @GetMapping("/refund/log/all")
    public Result getAllRefundLogs() {
        try {
            List<RefundVO> allRefundLogs = orderService.getAllRefundLogs();
            return Result.ok(allRefundLogs);
        } catch (Exception e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }
}