package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.constant.OrderStatus;
import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.CreateOrderVO;
import com.unicorn.journey.assistant.controller.vo.OrderVO;
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
    @Tool("Tool: 创建订单")
    public CreateOrderVO createOrder(CreateOrderRequest createOrderRequest) {
        Order order = OrderMapper.INSTANCE.convertToOrder(createOrderRequest);
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING.name());
        order.setTotalPrice(calculateTotalPrice(order));
        this.saveOrder(order);
        CreateOrderVO createOrderVO = new CreateOrderVO();
        createOrderVO.setOrderId(order.getId());
        return createOrderVO;
    }

    /**
     * 计算总价
     * @param order
     * @return
     */
    private int calculateTotalPrice(Order order) {
        int totalPrice = 0;
        for(Order.PurchasedProduct p : order.getPurchasedProducts()) {
            Product product = productService.getProductById(p.getProductId());
            OrderVO.PurchasedProductVO purchasedProductVO = new OrderVO.PurchasedProductVO();
            purchasedProductVO.setProductName(product.getProductName());
            purchasedProductVO.setQuantity(p.getQuantity());
            Product.InventoryCalendar calendar = Arrays.stream(product.getInventoryCalendar())
                    .filter(calendar1 -> calendar1.getDate().equals(order.getVisitDate()))
                    .findFirst()
                    .orElse(null);
            assert calendar != null;
            //价格
            int price = calendar.getPrice() * purchasedProductVO.getQuantity();
            totalPrice = totalPrice + price;
        }
        return totalPrice;
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
