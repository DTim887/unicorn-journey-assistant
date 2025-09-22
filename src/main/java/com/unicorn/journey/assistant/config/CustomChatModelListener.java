package com.unicorn.journey.assistant.config;

import com.unicorn.journey.assistant.service.TokenUsageTracker;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomChatModelListener implements ChatModelListener {

    private static final Logger logger = LoggerFactory.getLogger(CustomChatModelListener.class);

    @Resource
    private TokenUsageTracker tokenUsageTracker;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        List<ChatMessage> messages = requestContext.chatRequest().messages();
        logger.info("----- ChatModelListener: Request Received -----");
        messages.forEach(message -> logger.info("User Message: {}", message));
        logger.info("----------------------------------------------");
        // You could potentially modify the request here if needed
        // context.requestBuilder()... // Modify requestBuilder
        ChatModelListener.super.onRequest(requestContext);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        String responseText = responseContext.chatResponse().aiMessage().text();
        logger.info("----- ChatModelListener: Response Generated -----");
        logger.info("AI Response: {}", responseText);
        logger.info("Token Usage: {}", responseContext.chatResponse().tokenUsage());
        logger.info("Finish Reason: {}", responseContext.chatResponse().finishReason());
        logger.info("-----------------------------------------------");
        // 核心：记录 Token 使用情况
        tokenUsageTracker.recordTokenUsage(responseContext.chatResponse().tokenUsage());
        ChatModelListener.super.onResponse(responseContext);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        logger.info("error");
        ChatModelListener.super.onError(errorContext);
    }
}
