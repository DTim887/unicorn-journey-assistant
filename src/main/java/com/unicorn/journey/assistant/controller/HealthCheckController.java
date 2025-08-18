package com.unicorn.journey.assistant.controller;


import com.unicorn.journey.assistant.controller.vo.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {


    @GetMapping
    public ResponseEntity<Health> healthCheck() {
        return ResponseEntity.ok(Health.ok());
    }
}
