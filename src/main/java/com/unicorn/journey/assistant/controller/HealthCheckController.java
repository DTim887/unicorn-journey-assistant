package com.unicorn.journey.assistant.controller;


import com.unicorn.journey.assistant.controller.vo.Health;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    
    @GetMapping
    public ResponseEntity<Health> healthCheck() {
        logger.info("Health check endpoint called");
        return ResponseEntity.ok(Health.ok());
    }
}
