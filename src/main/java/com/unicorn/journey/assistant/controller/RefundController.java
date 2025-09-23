package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.RefundOrderRequest;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Refund;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.RefundService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RefundController {

    private RefundService refundService;

    private OrderService orderService;


    public RefundController(RefundService refundService, OrderService orderService) {
        this.refundService = refundService;
        this.orderService = orderService;
    }

    //根据订单号查询退款记录
    @GetMapping("/refund/detail/{orderId}")
    public Result getRefundByOrderId(@PathVariable String orderId) {
        Refund refund = refundService.get(orderId);
        return Result.ok(refund);
    }

    //创建退款记录
    @PostMapping("/refund/create")
    public Result createRefund(@RequestBody RefundOrderRequest refundOrderRequest) {
        refundService.refundOrder(refundOrderRequest);
        return Result.ok();
    }

    //根据用户ID查询退款记录
    @GetMapping("/refund/list")
    public Result getRefundByOrderId(@RequestParam int userId) {
        List<Refund> refunds = refundService.retrieveRefundsByUserId(userId);
        return Result.ok(refunds);
    }

}
