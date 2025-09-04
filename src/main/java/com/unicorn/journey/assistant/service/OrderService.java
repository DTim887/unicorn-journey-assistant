package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.constant.OrderStatus;
import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.CreateOrderVO;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.mappers.OrderMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@LocalCache(value = CacheName.ORDERS)
public class OrderService extends BaseService<Order> {

    private final ProductService productService;

    public OrderService(ProductService productService) {
        super();
        this.productService = productService;
    }

    public Order retrieveOrderById(String id) {
        return this.get(id);
    }

    /**
     * userId : 下单人的userId
     * purchasedProducts : 订单中包含的产品列表数组, 包含产品ID和产品数量
     * visitDate : 入园日期
     */
    @Tool("创建订单的工具")
    public CreateOrderVO createOrder(CreateOrderRequest createOrderRequest) {
        Order order = OrderMapper.INSTANCE.convertToOrder(createOrderRequest);
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING.name());
        this.saveOrder(order);
        CreateOrderVO createOrderVO = new CreateOrderVO();
        createOrderVO.setOrderId(order.getId());
        createOrderVO.setOrderLink("http://localhost:8080/journey-assistant/order-detail.html?orderId=" + order.getId());
        return createOrderVO;
    }

    @Tool("通过订单ID对订单进行退款的工具")
    public void refundOrder(String orderId) {
        Order order = this.get(orderId);
        order.setStatus(OrderStatus.REFUND.name());
        this.saveOrder(order);
    }



    public void saveOrder(Order order) {
        this.put(order.getId(), order);
    }

    public Order getOrder(String orderId) {
        return this.get(orderId);
    }

    public List<Order> retrieveOrdersByUserId(int userId) {
        List<Order> orders = this.getAll(Order.class);
        if (!CollectionUtils.isEmpty(orders)) {
            return orders.stream()
                    .filter(order -> userId == order.getUserId())
                    .collect(Collectors.toList());
        }
        return null;
    }
}
