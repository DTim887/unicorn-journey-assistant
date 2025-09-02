package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.mappers.OrderMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
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

    @Tool("生成订单")
    /**
     * userId : 下单人id
     * productId : 下单产品id
     * quantity : 下单总数量，比如买了三张一日票则 quantity = 3
     * visitDate : 入园日期
     * @param createOrderRequest
     */
    public void saveOrder(CreateOrderRequest createOrderRequest) {
        Order order = OrderMapper.INSTANCE.convertToOrder(createOrderRequest);
        order.setId(UUID.randomUUID().toString());
        //计算订单总价格
        Product product = productService.getProductById(order.getProductId());
        Product.Calendar calendar = Arrays.stream(product.getCalendar())
                .filter(calendar1 -> calendar1.getDate().equals(order.getVisitDate()))
                .findFirst()
                .orElse(null);
        assert calendar != null;
        order.setTotalPrice(calendar.getPrice() * order.getQuantity());
        this.put(order.getId(), order);
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
