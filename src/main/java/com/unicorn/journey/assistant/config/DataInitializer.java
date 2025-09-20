package com.unicorn.journey.assistant.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.entity.Assistant;
import com.unicorn.journey.assistant.entity.Attraction;
import com.unicorn.journey.assistant.entity.Order;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.AssistantService;
import com.unicorn.journey.assistant.service.AttractionService;
import com.unicorn.journey.assistant.service.OrderService;
import com.unicorn.journey.assistant.service.ProductService;
import com.unicorn.journey.assistant.service.UserService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@org.springframework.core.annotation.Order(value = 1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource
    private UserService userService;

    @Resource
    private AttractionService attractionService;

    @Resource
    private ProductService productService;

    @Resource
    private AssistantService assistantService;
    
    @Resource
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Load the JSON file from the classpath (src/main/resources)
        log.info("预热 user 缓存 Start");
        ClassPathResource resource = new ClassPathResource("user.json");
        if (!resource.exists()) {
            log.error("文件不存在: users.json");
            return;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {
            });
            users.forEach(userService::saveUser);
            //预设当前Seval是登录用户
            User currentUser = userService.retrieveUserByNickname("Seval");
            if (currentUser != null) {
                userService.login(currentUser);
            }
            log.info("预热 user 缓存 End");
        }

        log.info("预热 attraction 缓存 Start");
        ClassPathResource attractionResource = new ClassPathResource("attraction.json");
        if (!attractionResource.exists()) {
            log.error("文件不存在: attraction.json");
            return;
        }


        try (InputStream attractionInputStream = attractionResource.getInputStream()) {
            List<Attraction> attractions = objectMapper.readValue(attractionInputStream, new TypeReference<List<Attraction>>() {
            });

            Random random = new Random();

            attractions.forEach(attraction -> {
                Integer queueTime = attraction.getQueueTime();

                // 只有 queueTime == 0 时不赋值，其他情况都赋值
                if (queueTime == null || queueTime != 0) {
                    int newQueueTime = random.nextInt(10, 121);
                    attraction.setQueueTime(newQueueTime);
                }

                // 保存修改后的对象
                attractionService.saveAttraction(attraction);
            });
            log.info("最终景点信息为：{}", attractions);
            log.info("预热 attraction 缓存 End");
        }

        warmupProduct();
        warmupAssistant();
        // 添加初始化订单的逻辑
        initializeOrder();
    }

    private void initializeOrder() {
        log.info("初始化订单 Start");
        try {
            // 直接创建Order对象
            Order order = new Order();
            order.setId(UUID.randomUUID().toString());
            order.setUserId(2); // 用户ID为2
            // 动态计算明天的日期
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            order.setVisitDate(tomorrow.toString()); // 入园日期设为明天
            order.setStatus("PENDING"); // 待付款状态
            
            // 创建购买产品列表
            List<Order.PurchasedProduct> purchasedProducts = new ArrayList<>();
            
            // 添加第一个产品
            Order.PurchasedProduct product1 = new Order.PurchasedProduct();
            product1.setProductId(1);
            product1.setQuantity(1);
            purchasedProducts.add(product1);
            
            // 添加第二个产品
            Order.PurchasedProduct product2 = new Order.PurchasedProduct();
            product2.setProductId(2);
            product2.setQuantity(2);
            purchasedProducts.add(product2);
            
            order.setPurchasedProducts(purchasedProducts);
            
            // 计算并设置订单总价
            int totalPrice = orderService.calculateOrderTotalPrice(order);
            order.setTotalPrice(totalPrice);
            
            // 保存订单
            orderService.saveOrder(order);
            log.info("订单初始化成功，订单ID：{}，订单总价：{}", order.getId(), totalPrice);
        } catch (Exception e) {
            log.error("初始化订单失败：{}", e.getMessage());
        }
        log.info("初始化订单 End");
    }

    public void warmupAssistant() throws IOException {
        log.info("预热 assistant 缓存 Start");
        ClassPathResource resource = new ClassPathResource("assistant.json");
        if (!resource.exists()) {
            log.error("文件不存在: assistant.json");
            return;
        }
        try (InputStream productInputStream = resource.getInputStream()) {
            List<Assistant> assistants = objectMapper.readValue(productInputStream, new TypeReference<>() {});
            assistants.forEach(assistant -> assistantService.saveAssistant(assistant));
            //预设当前Judy是当前助手
            Assistant assistant = assistantService.retrieveAssistantByName("小熊维尼");
            if (assistant != null) {
                assistantService.exchange(assistant);
            }
            log.info("预热 assistant 缓存 End, size:{}", assistants.size());
        }
    }

    private void warmupProduct() throws IOException {
        log.info("预热 product 缓存 Start");
        ClassPathResource productResource = new ClassPathResource("product.json");
        if (!productResource.exists()) {
            log.error("文件不存在: product.json");
            return;
        }
        try (InputStream productInputStream = productResource.getInputStream()) {
            List<Product> products = objectMapper.readValue(productInputStream, new TypeReference<>() {});
            
            // 为每个产品生成从当天开始到一个月后的库存日历
            LocalDate today = LocalDate.now();
            LocalDate oneMonthLater = today.plusMonths(1);
            
            products.forEach(product -> {
                // 获取产品的原始库存日历，如果有，用于参考价格
                Integer defaultPrice = 475; // 默认价格
                if (product.getInventoryCalendar() != null && product.getInventoryCalendar().length > 0) {
                    defaultPrice = product.getInventoryCalendar()[0].getPrice();
                }
                
                // 生成新的库存日历数组
                List<Product.InventoryCalendar> newInventoryCalendars = new ArrayList<>();
                LocalDate currentDate = today;
                
                while (!currentDate.isAfter(oneMonthLater)) {
                    Product.InventoryCalendar inventoryCalendar = new Product.InventoryCalendar();
                    inventoryCalendar.setDate(currentDate.toString());
                    inventoryCalendar.setPrice(defaultPrice);
                    inventoryCalendar.setInventory(100); // 设置默认库存为100
                    newInventoryCalendars.add(inventoryCalendar);
                    
                    // 移动到下一天
                    currentDate = currentDate.plusDays(1);
                }
                
                // 将List转换为数组并设置到产品中
                product.setInventoryCalendar(newInventoryCalendars.toArray(new Product.InventoryCalendar[0]));
                
                // 保存产品
                productService.saveProduct(product);
            });
            
            log.info("预热 product 缓存 End, size:{}", products.size());
        }
    }
}
