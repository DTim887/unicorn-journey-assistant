package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.constant.Assistants;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Assistant;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.enums.VoiceCharacter;
import com.unicorn.journey.assistant.service.AssistantService;
import com.unicorn.journey.assistant.service.STTService;
import com.unicorn.journey.assistant.service.TrainingTTSService;
import com.unicorn.journey.assistant.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Log4j2
@RestController
public class ChatController {

    private final AiServiceFactory aiServiceFactory;

    private final UserService userService;

    private final STTService sttService;

    private final TrainingTTSService trainingTTSService;

    private final AssistantService assistantService;

    public ChatController(AiServiceFactory aiServiceFactory, UserService userService, STTService sttService, TrainingTTSService trainingTTSService, AssistantService assistantService) {
        this.aiServiceFactory = aiServiceFactory;
        this.userService = userService;
        this.sttService = sttService;
        this.trainingTTSService = trainingTTSService;
        this.assistantService = assistantService;
    }

    //和朱迪聊天
    @GetMapping(value = "/judy-chat")
    public Flux<String> judyChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getDeepseekAiService(memoryId, Assistants.JUDY);
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
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
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.duffyChat(memoryId, userMessage, user);
    }

    //和 Duffy 文字聊天
    @GetMapping("/duffy-text")
    public Flux<String> wennieTextChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.DUFFY);
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.duffyChat(memoryId, userMessage, user);
    }

    //和 Wennie 聊天
    @GetMapping("/wennie-chat")
    public Flux<String> wennieChat(@RequestParam String userMessage) {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getAiService(memoryId, Assistants.WENNIE);
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.wennieChat(memoryId, userMessage, user);
    }


    //和 woody 聊天
    @GetMapping("/woody-chat")
    public Flux<String> woodyChat(@RequestParam String userMessage) throws Exception {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        //Remembering the current logged-in user
        AiService aiService = aiServiceFactory.getDeepseekAiService(memoryId, Assistants.WOODY);
        log.info("Send text:{}, memoryId:{} ", userMessage, memoryId);
        return aiService.woodyChat(memoryId, userMessage, user);
    }

    @PutMapping("/new-conversation")
    public Result newConversation() {
        User user = userService.currentUser();
        Assistant assistant = assistantService.currentAssistant();
        String memoryId = assistant.getAssistantName() + user.getId();
        aiServiceFactory.newConversation(memoryId);
        return Result.ok();
    }



    @GetMapping("/tts")
    public Result tts() throws Exception {
        sttService.textToSpeechAndSave("我是一只小小小小鸟。想要飞却怎么也飞不高。", VoiceCharacter.NICK);
        return Result.ok();
    }




    @GetMapping("/ttts")
    public Result ttts() throws Exception {
        sttService.textToSpeechAndSave("这叫智取，宝贝儿。",VoiceCharacter.NICK);
        return Result.ok();
    }

}
