package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.client.AudioRequest;
import com.unicorn.journey.assistant.client.ByteDanceRecognizeClient;
import com.unicorn.journey.assistant.client.SSTResponse;
import com.unicorn.journey.assistant.utils.Base64Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class STTService {

    private final ByteDanceRecognizeClient byteDanceRecognizeClient;

    @Value("${doubao.app.id}")
    private String appId;

    @Value("${doubao.app.accesstoken}")
    private String accessToken;

    @Value("${doubao.sst.resource.id}")
    private String resourceId;

    private static final int SEQUENCE = -1;
    private static final String MODEL_NAME = "bigmodel";

    public STTService(ByteDanceRecognizeClient byteDanceRecognizeClient) {
        this.byteDanceRecognizeClient = byteDanceRecognizeClient;
    }

    public String speechToText(MultipartFile file) throws IOException {
        //组装豆包大模型语音转换请求
        AudioRequest audioRequest = AudioRequest.builder()
                .audio(new AudioRequest.Audio(Base64Utils.convertToBase64(file)))
                .user(new AudioRequest.User(appId))
                .request(new AudioRequest.Request(MODEL_NAME)).
                build();
        //组装返回
        SSTResponse sstResponse = byteDanceRecognizeClient.recognizeFlash(
                appId,
                accessToken,
                resourceId,
                SEQUENCE,
                audioRequest);
        return sstResponse.getResult().getText();
    }
}
