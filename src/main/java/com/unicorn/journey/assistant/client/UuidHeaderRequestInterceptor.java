package com.unicorn.journey.assistant.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class UuidHeaderRequestInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UuidHeaderRequestInterceptor.class);
    /**
     * Header 名称常量
     */
    public static final String X_API_REQUEST_ID = "X-Api-Request-Id"; // 你可以根据需要修改 Header 名称

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 生成随机 UUID
        String uuid = UUID.randomUUID().toString();

        // 将 UUID 添加到请求 Header 中
        requestTemplate.header(X_API_REQUEST_ID, uuid);

        // 可选：记录日志以便调试
        logger.debug("Added {} header with value {} to request to {}", X_API_REQUEST_ID, uuid, requestTemplate.url());
    }
}
