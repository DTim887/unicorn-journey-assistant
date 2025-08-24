package com.unicorn.journey.assistant.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.UserService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Order(value = 1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource
    UserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
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
            //预设当前Tim是登录用户
            User currentUser = userService.retrieveUserByNickname("Tim");
            if (currentUser != null) {
                userService.login(currentUser);
            }
            log.info("预热 user 缓存 End");
        }
    }
}
