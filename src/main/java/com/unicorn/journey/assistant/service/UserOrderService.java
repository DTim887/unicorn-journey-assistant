package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@LocalCache(value = CacheName.ORDERS)
public class UserOrderService extends BaseService<List<Order>> {

    public void init(int userId, List<Order> orders) {
        this.put(userId, orders);
    }

    //通过用户ID保存订单
    public void saveOrder(int userId, Order order) {
        List<Order> orderList = this.get(userId);
        if(orderList==null){
            orderList = new ArrayList<>();
        }
        orderList.add(order);
        this.put(userId, orderList);
    }

    //通过用户ID获得订单列表
    public List<Order> retrieveOrdersByUserId(int userId) {
        return this.get(userId);
    }

}
