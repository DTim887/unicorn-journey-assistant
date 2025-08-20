package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.ORDERS)
public class OrderService extends BaseService<Order> {

    public void saveOrder(Order order) {
        this.put(order.getId(), order);
    }

    public Order retrieveOrderById(int id) {
        return this.get(id);
    }

    public List<Order> retrieveAllOrders() {
        return this.getAll(Order.class);
    }
}
