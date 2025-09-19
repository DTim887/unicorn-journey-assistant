package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.CreateOrderVO;
import com.unicorn.journey.assistant.controller.vo.OrderVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.mappers.OrderMapper;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.ProductService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
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
    @PostMapping("/order/create")
    public Result createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        CreateOrderVO createOrderVO = orderService.createOrder(createOrderRequest);
        return Result.ok(createOrderVO);
    }

    //根据用户ID获取订单列表 - 用户 id 是订单实体属性, service 层做 filter
    @GetMapping("/order/list")
    public Result getOrdersByUser(@RequestParam int userId) {
        List<Order> orders = orderService.retrieveOrdersByUserId(userId);
        return Result.ok(orders);
    }

    //根据订单id获取订单
    @GetMapping("/order/detail/{orderId}")
    public Result getOrderById(@PathVariable String orderId) {
        try {
            Order order = orderService.retrieveOrderById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found with ID: " + orderId);
            }
            OrderVO orderVO = OrderMapper.INSTANCE.convertToOrderVO(order);
            List<OrderVO.PurchasedProductVO> purchasedProductVOs = new ArrayList<>();
            order.getPurchasedProducts().forEach(p -> {
                Product product = productService.getProductById(p.getProductId());
                OrderVO.PurchasedProductVO purchasedProductVO = new OrderVO.PurchasedProductVO();
                purchasedProductVO.setProductName(product.getProductName());
                purchasedProductVO.setQuantity(p.getQuantity());
                Product.InventoryCalendar calendar = Arrays.stream(product.getInventoryCalendar())
                        .filter(calendar1 -> calendar1.getDate().equals(order.getVisitDate()))
                        .findFirst()
                        .orElse(null);
                if (calendar == null) {
                    throw new IllegalArgumentException("No inventory found for visit date " + order.getVisitDate() + " and product " + product.getProductName());
                }
                //价格
                int price = calendar.getPrice() * purchasedProductVO.getQuantity();
                //单个产品价格
                purchasedProductVO.setPrice(price);
                orderVO.setTotalPrice(orderVO.getTotalPrice() + price);
                purchasedProductVOs.add(purchasedProductVO);
            });
            orderVO.setPurchasedProductVOs(purchasedProductVOs);
            return Result.ok(orderVO);
        } catch (IllegalArgumentException e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }

    //订单退款
    @PostMapping("/order/refund")
    public Result refundOrder(@RequestBody OrderRefundRequest refundRequest) {
        try {
            orderService.refundOrder(refundRequest.getOrderId());
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }

    //订单退款请求类
    @Getter
    @Setter
    public static class OrderRefundRequest {
        private String orderId;
    }
}
