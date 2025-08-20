package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class TestController {

    private final AiService aiService;

    public TestController(AiService aiService) {
        this.aiService = aiService;
    }


    @GetMapping("/test")
    public Flux<String> test(@RequestParam String userMessage) {
        return aiService.streamChat(1,userMessage);
    }
}
