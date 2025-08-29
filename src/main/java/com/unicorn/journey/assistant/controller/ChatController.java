package com.unicorn.journey.assistant.controller;

import com.google.cloud.speech.v1.RecognitionConfig;
import com.unicorn.journey.assistant.chat.AiService;
import com.unicorn.journey.assistant.chat.AiServiceFactory;
import com.unicorn.journey.assistant.client.AudioRequest;
import com.unicorn.journey.assistant.client.ByteDanceRecognizeClient;
import com.unicorn.journey.assistant.client.SSTResponse;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.UserService;
import com.unicorn.journey.assistant.utils.Base64Utils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
public class ChatController {

    @Resource
    ByteDanceRecognizeClient byteDanceRecognizeClient;

    @Resource
    RecognitionConfig recognitionConfig;

    @Value("${doubao.app.id}")
    private String appId;

    @Value("${doubao.app.accesstoken}")
    private String accesstoken;

    @Value("${doubao.sst.resource.id}")
    private String resourceId;

    private static final int SEQUENCE = -1;
    private static final String MODEL_NAME = "bigmodel";

    private final AiServiceFactory aiServiceFactory;

    private final UserService userService;

    private final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(AiServiceFactory aiServiceFactory, UserService userService) {
        this.aiServiceFactory = aiServiceFactory;
        this.userService = userService;
    }


    // talk to LLM
    @GetMapping("/chat-stream")
    public Flux<String> chatStream(@RequestParam String userMessage) {
        User user = userService.currentUser();
        AiService aiService = aiServiceFactory.getAiService(user);
        //Remembering the current logged-in user
        logger.info("Memory ID: {}, userMessage: {}, Nickname:{}", user.getId(), userMessage, user.getNickname());
        return aiService.streamChat(user.getId(), userMessage, user.getNickname());
    }

    // voice chat
    @PostMapping("/voice-chat")
    public Flux<String> voiceChat(@RequestParam("audio") MultipartFile file) throws IOException {
        //组装豆包大模型语音转换请求
        AudioRequest audioRequest = AudioRequest.builder()
                .audio(new AudioRequest.Audio(Base64Utils.convertToBase64(file)))
                .user(new AudioRequest.User(appId))
                .request(new AudioRequest.Request(MODEL_NAME)).
                build();
        //组装返回
        SSTResponse sstResponse = byteDanceRecognizeClient.recognizeFlash(
                appId,
                accesstoken,
                resourceId,
                SEQUENCE,
                audioRequest);
        User user = userService.currentUser();
        AiService aiService = aiServiceFactory.getAiService(user);
        //把语音转文字作为大模型的输入
        return aiService.streamChat(user.getId(), sstResponse.getResult().getText(), user.getNickname());
    }

}
