package com.unicorn.journey.assistant.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.entity.*;
import com.unicorn.journey.assistant.service.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

@Component
@Order(value = 1)
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
    private RedNoteService redNoteService;

    @Resource
    private DpaService dpaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Load the JSON file from the classpath (src/main/resources)
        warmupUser();
        warmupAttraction();
        warmupProduct();
        warmupAssistant();
        wrmupRednote();
        wrmupDpa();
    }

    public void warmupUser() throws IOException {
        log.info("预热 user 缓存 Start");
        ClassPathResource resource = new ClassPathResource("user.json");
        if (!resource.exists()) {
            log.error("文件不存在: users.json");
            return;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            users.forEach(userService::saveUser);
            //预设当前Tim是登录用户
            User currentUser = userService.retrieveUserByNickname("Tim");
            if (currentUser != null) {
                userService.login(currentUser);
            }
            log.info("预热 user 缓存 End");
        }
    }

    public void warmupAttraction() throws IOException {
        log.info("预热 attraction 缓存 Start");
        ClassPathResource attractionResource = new ClassPathResource("attraction.json");
        if (!attractionResource.exists()) {
            log.error("文件不存在: attraction.json");
            return;
        }


        try (InputStream attractionInputStream = attractionResource.getInputStream()) {
            List<Attraction> attractions = objectMapper.readValue(attractionInputStream, new TypeReference<>() {
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
            log.info("预热 attraction 缓存 End");
        }
    }

    public void warmupAssistant() throws IOException {
        log.info("预热 assistant 缓存 Start");
        ClassPathResource resource = new ClassPathResource("assistant.json");
        if (!resource.exists()) {
            log.error("文件不存在: assistant.json");
            return;
        }
        try (InputStream productInputStream = resource.getInputStream()) {
            List<Assistant> assistants = objectMapper.readValue(productInputStream, new TypeReference<>() {
            });
            assistants.forEach(assistant -> assistantService.saveAssistant(assistant));
            //预设当前Judy是当前助手
            Assistant assistant = assistantService.retrieveAssistantByName("朱迪");
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
            List<Product> products = objectMapper.readValue(productInputStream, new TypeReference<>() {
            });
            products.forEach(product -> productService.saveProduct(product));
            log.info("预热 product 缓存 End, size:{}", products.size());
        }
    }

    private void wrmupRednote() throws IOException {
        log.info("预热 rednote 缓存 Start");
        ClassPathResource rednoteResource = new ClassPathResource("rednote.json");
        if (!rednoteResource.exists()) {
            log.error("文件不存在: rednote.json");
            return;
        }
        try (InputStream rednoteResourceInputStream = rednoteResource.getInputStream()) {
            List<RedNote> redNotes = objectMapper.readValue(rednoteResourceInputStream, new TypeReference<>() {
            });
            redNotes.forEach(redNote -> redNoteService.saveRedNote(redNote));
            log.info("预热 rednote 缓存 End, size:{}", redNotes.size());
        }
    }


    private void wrmupDpa() throws IOException {
        log.info("预热 DPA 缓存 Start");
        ClassPathResource rednoteResource = new ClassPathResource("dpa.json");
        if (!rednoteResource.exists()) {
            log.error("文件不存在: dpa.json");
            return;
        }
        try (InputStream rednoteResourceInputStream = rednoteResource.getInputStream()) {
            List<DPA> dpaList = objectMapper.readValue(rednoteResourceInputStream, new TypeReference<>() {
            });
            dpaList.forEach(dpa -> dpaService.saveDpa(dpa));
            log.info("预热 dpa 缓存 End, size:{}", dpaList.size());
        }
    }
}
