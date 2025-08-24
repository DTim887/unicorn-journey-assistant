package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final AiServiceFactory aiServiceFactory;

    private final UserService userService;

    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(AiServiceFactory aiServiceFactory, UserService userService) {
        this.aiServiceFactory = aiServiceFactory;
        this.userService = userService;
    }


    // talk to LLM
    @CrossOrigin(origins = "*")
    @GetMapping("/chat-stream")
    public Flux<String> chatStream(@RequestParam String userMessage) {
        User user = userService.currentUser();
        AiService aiService = aiServiceFactory.getAiService(user);
        //Remembering the current logged-in user
        logger.info("Memory ID: {}, userMessage: {}, Nickname:{}", user.getId(), userMessage, user.getNickname());
        return aiService.streamChat(user.getId(), userMessage, user.getNickname());
    }

}
