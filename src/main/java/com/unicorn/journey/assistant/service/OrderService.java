package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@LocalCache(value = CacheName.ORDERS)
public class OrderService extends BaseService<Order>{

    public Order retrieveOrderById(int id) {
        return this.get(id);
    }

    public void saveOrder(Order order) {
        this.put(order.getId(),order);
    }

    public List<Order> retrieveOrdersByUserId(int userId) {
        List<Order> orders = this.getAll(Order.class);
        if(!CollectionUtils.isEmpty(orders)){
            return orders.stream()
                    .filter(order -> userId == order.getUserId())
                    .collect(Collectors.toList());
        }
        return null;
    }
}
