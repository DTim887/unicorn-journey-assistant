package com.unicorn.journey.assistant.config;

import com.google.cloud.speech.v1.RecognitionConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class STTConfiguration {


    // 没有谷歌Cloud账号暂时无法使用
    @Bean
    public RecognitionConfig recognitionConfig() {
        return RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // 根据你的音频文件格式调整
                .setSampleRateHertz(16000) // 根据你的音频文件采样率调整
                .setLanguageCode("zh-CN") // 设置为中文
                .build();
    }

}
