package com.unicorn.journey.assistant.config;


import com.unicorn.journey.assistant.client.UuidHeaderRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInterceptorConfiguration {

    @Bean
    public RequestInterceptor uuidHeaderRequestInterceptor() {
        return new UuidHeaderRequestInterceptor(); // 返回 Interceptor 实例
    }
}
