package com.unicorn.journey.assistant;

import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class UnicornJourneyAssistantApplicationTests {


    @Resource
    private UserService userService;

    @Resource
    private OrderService orderService;

    @Test
    void contextLoads() {
//        User getUser1 = userService.retrieveUserById(1);
//        User user = User.builder().id(1).nickname("Tim").build();
//        userService.saveUser(user);
//        User user1 = User.builder().id(2).nickname("Seval").build();
//        userService.saveUser(user1);
//        User getUser2 = userService.retrieveUserById(2);
//        List<User> users = userService.retrieveAllUsers();

//        CaffeineCacheManager cacheManager = SpringBeanUtils.getBean(CaffeineCacheManager.class);
//        CaffeineCache nativeCache =  (CaffeineCache)cacheManager.getCache("USERS");
//        Assertions.assertNotNull(cache);
//        CaffeineCache nativeCache = (CaffeineCache) cache.getNativeCache();
//        System.out.println(users);

        Order order = Order.builder().id(1).orderNo("CH112233").build();
        Order order1 = Order.builder().id(2).orderNo("CC11222").build();
        Order order2 = Order.builder().id(3).orderNo("CS1111").build();
        orderService.saveOrder(order);
        orderService.saveOrder(order1);
        orderService.saveOrder(order2);
        List<Order> orders = orderService.retrieveAllOrders();
        System.out.println(orders);

    }

}
