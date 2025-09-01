package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.OrderVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.mappers.OrderMapper;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;


    public OrderController(OrderService orderService, ProductService productService) {
        this.orderService = orderService;
        this.productService = productService;
    }

    //存储订单
    @PostMapping("/order/save")
    public Result saveOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        orderService.saveOrder(createOrderRequest);
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
    public Result getOrderById(@PathVariable String orderId) {
        Order order = orderService.retrieveOrderById(orderId);
        Product product = productService.getProductById(order.getProductId());
        OrderVO orderVO = OrderMapper.INSTANCE.convertToOrderVO(order);
        orderVO.setProductName(product.getProductName());
        return Result.ok(orderVO);
    }
}
