package com.unicorn.journey.assistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置动态语音文件访问
        // 将 /voice/** 请求映射到应用运行目录的 voice 文件夹
        String voiceDir = System.getProperty("user.dir") + File.separator + "voice" + File.separator;
        
        registry.addResourceHandler("/voice/**")
                .addResourceLocations("file:" + voiceDir)
                .setCachePeriod(3600); // 缓存1小时
        
        // 保持默认的静态资源处理
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
