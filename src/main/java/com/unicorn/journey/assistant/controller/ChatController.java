package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.entity.Assistant;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.AssistantService;
import com.unicorn.journey.assistant.service.STTService;
import com.unicorn.journey.assistant.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
public class ChatController {


    private final AiServiceFactory aiServiceFactory;

    private final UserService userService;

    private final STTService sttService;

    private final AssistantService assistantService;

    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(AiServiceFactory aiServiceFactory, UserService userService, STTService sttService, AssistantService assistantService) {
        this.aiServiceFactory = aiServiceFactory;
        this.userService = userService;
        this.sttService = sttService;
        this.assistantService = assistantService;
    }

    //和朱迪聊天
    @GetMapping("/judy-chat")
    public Flux<String> judyChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.JUDY);
        logger.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.judyChat(memoryId, userMessage, user);
    }

    //和 Duffy 聊天
    @PostMapping("/duffy-chat")
    public Flux<String> voiceChat(@RequestParam("audio") MultipartFile file) throws IOException {
        String userMessage = sttService.speechToText(file);
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //把语音转文字作为大模型的输入
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.DUFFY);
        logger.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.duffyChat(memoryId, userMessage, user);
    }
    
    //和 Stella 聊天
    @GetMapping("/stella-chat")
    public Flux<String> stellaChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.JUDY); // 使用JUDY作为临时助理类型
        logger.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.stellaChat(memoryId, userMessage);
    }

    //和 Wennie 聊天
    @GetMapping("/wennie-chat")
    public Flux<String> wennieChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.WENNIE);
        logger.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.wennieChat(memoryId, userMessage, user);
    }

}
