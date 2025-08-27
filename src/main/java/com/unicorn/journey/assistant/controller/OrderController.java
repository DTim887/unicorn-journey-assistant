package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;


    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //存储订单
    @PostMapping("/order/save")
    public Result saveOrder(@RequestBody Order order) {
        orderService.saveOrder(order);
        return Result.ok();
    }

    //根据用户ID获取订单列表 - 用户 id 是订单实体属性, service 层做 filter
    @GetMapping("/order/get")
    public Result getOrdersByUser(@RequestParam int userId) {
        List<Order> orders = orderService.retrieveOrdersByUserId(userId);
        return Result.ok(orders);
    }

    //根据订单id获取订单
    @GetMapping("/order/get/{orderId}")
    public Result getOrderById(@PathVariable int orderId) {
        Order order = orderService.retrieveOrderById(orderId);
        return Result.ok(order);
    }
}
