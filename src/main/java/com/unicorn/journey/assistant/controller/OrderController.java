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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final OrderMapper orderMapper;

    @Autowired
    public OrderController(OrderService orderService, ProductService productService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.productService = productService;
        this.orderMapper = orderMapper;
    }

    // 存储订单
    @PostMapping("/order/create")
    public Result createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        CreateOrderVO createOrderVO = orderService.createOrder(createOrderRequest);
        return Result.ok(createOrderVO);
    }

    // 根据用户ID获取订单列表 - 用户 id 是订单实体属性, service 层做 filter
    @GetMapping("/order/list")
    public Result getOrdersByUser(@RequestParam int userId) {
        List<Order> orders = orderService.retrieveOrdersByUserId(userId);
        return Result.ok(orders);
    }

    // 根据订单id获取订单
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

                // 从产品的库存日历中找到对应入园日期的价格
                int productPrice = 0;
                for (Product.InventoryCalendar calendar : product.getInventoryCalendar()) {
                    if (calendar.getDate().equals(order.getVisitDate())) {
                        productPrice = calendar.getPrice();
                        break;
                    }
                }

                if (productPrice == 0) {
                    throw new IllegalArgumentException("No inventory found for visit date " + order.getVisitDate()
                            + " and product " + product.getProductName());
                }

                // 计算单个产品的总价
                int price = productPrice * p.getQuantity();
                purchasedProductVO.setPrice(price);
                purchasedProductVOs.add(purchasedProductVO);
            });
            orderVO.setPurchasedProductVOs(purchasedProductVOs);
            // 设置订单总价
            orderVO.setTotalPrice(order.getTotalPrice());
            return Result.ok(orderVO);
        } catch (IllegalArgumentException e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }

    // 订单退款
    @PostMapping("/order/refund")
    public Result refundOrder(@RequestBody OrderRefundRequest refundRequest) {
        try {
            Order order = orderService.retrieveOrderById(refundRequest.getOrderId());
            if (order == null) {
                throw new IllegalArgumentException("Order not found with ID: " + refundRequest.getOrderId());
            }

            // 直接使用订单中已有的totalPrice字段
            int totalPrice = order.getTotalPrice();

            // 根据退款百分比计算退款金额
            int refundPercentage = refundRequest.getRefundPercentage();
            int refundAmount = (int) (totalPrice * (refundPercentage / 100.0));

            // 确定退款类型
            String refundType = refundPercentage == 100 ? "全额退款" : "折扣退款";

            orderService.refundOrder(refundRequest.getOrderId(), refundAmount, refundType);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.builder().code("1").msg(e.getMessage()).build();
        }
    }

    // 订单退款请求类
    // 订单退款请求类
    @Getter
    @Setter
    public static class OrderRefundRequest {
        private String orderId;
        private int refundPercentage;
    }
}
