package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.constant.OrderStatus;
import com.unicorn.journey.assistant.controller.request.CreateOrderRequest;
import com.unicorn.journey.assistant.controller.vo.CreateOrderVO;
import com.unicorn.journey.assistant.controller.vo.RefundVO;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.mappers.OrderMapper;
import com.unicorn.journey.assistant.entity.Product;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 订单服务类
 */
@Service
@LocalCache(value = CacheName.ORDERS)
public class OrderService extends BaseService<Order> {

    private final ProductService productService;
    private final OrderMapper orderMapper;
    // 退款日志缓存
    private final Map<String, List<RefundVO>> refundLogCache = new ConcurrentHashMap<>();

    @Autowired
    public OrderService(ProductService productService, OrderMapper orderMapper) {
        super();
        this.productService = productService;
        this.orderMapper = orderMapper;
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
        Order order = orderMapper.convertToOrder(createOrderRequest);
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING.name());
        
        // 计算并设置订单总价
        int totalPrice = calculateOrderTotalPrice(order);
        order.setTotalPrice(totalPrice);
        
        this.saveOrder(order);
        CreateOrderVO createOrderVO = new CreateOrderVO();
        createOrderVO.setOrderId(order.getId());
        createOrderVO.setOrderLink("order-detail.html?orderId=" + order.getId());
        return createOrderVO;
    }
    
    /**
     * 计算订单总价
     * @param order 订单对象
     * @return 订单总价
     */
    // 将private改为public，使DataInitializer能够调用该方法
    public int calculateOrderTotalPrice(Order order) {
        int totalPrice = 0;
        if (order != null && order.getPurchasedProducts() != null) {
            for (Order.PurchasedProduct purchasedProduct : order.getPurchasedProducts()) {
                Product product = productService.getProductById(purchasedProduct.getProductId());
                if (product != null && product.getInventoryCalendar() != null) {
                    for (Product.InventoryCalendar calendar : product.getInventoryCalendar()) {
                        if (calendar != null && calendar.getDate().equals(order.getVisitDate())) {
                            totalPrice += calendar.getPrice() * purchasedProduct.getQuantity();
                            break;
                        }
                    }
                }
            }
        }
        return totalPrice;
    }

    /**
     * 退款订单
     * @param orderId 订单ID
     * @param refundAmount 退款金额
     * @param refundType 退款类型：全额退，差价退
     * @return 退款结果
     */
    @Tool("通过订单ID对订单进行退款的工具")
    public void refundOrder(String orderId, int refundAmount, String refundType) {
        Order order = this.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found with ID: " + orderId);
        }
        order.setStatus(OrderStatus.REFUND.name());
        this.saveOrder(order);
        
        // 使用订单中已有的totalPrice字段
        saveRefundLog(orderId, order.getTotalPrice(), refundAmount, refundType);
    }
    
    /**
     * 保存订单
     * @param order 订单信息
     */
    public void saveOrder(Order order) {
        this.put(order.getId(), order);
    }

    /**
     * 获取订单
     * @param orderId 订单ID
     * @return 订单信息
     */
    public Order getOrder(String orderId) {
        return this.get(orderId);
    }

    /**
     * 根据用户ID获取订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @Tool("通过用户ID来查询用户的订单列表的工具")
    public List<Order> retrieveOrdersByUserId(int userId) {
        List<Order> orders = this.getAll(Order.class);
        if (!CollectionUtils.isEmpty(orders)) {
            return orders.stream()
                    .filter(order -> userId == order.getUserId())
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 获取所有订单
     * @return 所有订单列表
     */
    public List<Order> retrieveAllOrders() {
        return this.getAll(Order.class);
    }

    /**
     * 保存退款日志
     * @param orderId 订单ID
     * @param totalPrice 订单总价
     * @param refundAmount 退款金额
     * @param refundType 退款类型
     */
    private void saveRefundLog(String orderId, int totalPrice, int refundAmount, String refundType) {
        RefundVO refundLog = new RefundVO();
        refundLog.setOrderId(orderId);
        refundLog.setRefundPrice(refundAmount);
        refundLog.setType(refundType);
        refundLog.setRefundTime(LocalDateTime.now());
        refundLog.setStatus("SUCCESS");
        refundLog.setTotalPrice(totalPrice);
        
        List<RefundVO> logs = refundLogCache.getOrDefault(orderId, new ArrayList<>());
        logs.add(refundLog);
        refundLogCache.put(orderId, logs);
    }

    /**
     * 根据订单ID获取退款日志
     * @param orderId 订单ID
     * @return 退款日志列表
     */
    public List<RefundVO> getRefundLogsByOrderId(String orderId) {
        return refundLogCache.getOrDefault(orderId, new ArrayList<>());
    }

    /**
     * 获取所有退款日志
     * @return 所有退款日志列表
     */
    public List<RefundVO> getAllRefundLogs() {
        List<RefundVO> allLogs = new ArrayList<>();
        refundLogCache.values().forEach(allLogs::addAll);
        return allLogs;
    }
}
