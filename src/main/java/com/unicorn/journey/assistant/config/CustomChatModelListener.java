package com.unicorn.journey.assistant.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomChatModelListener implements ChatModelListener {

    private static final Logger logger = LoggerFactory.getLogger(CustomChatModelListener.class);

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
        ChatModelListener.super.onResponse(responseContext);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        logger.info("error");
        ChatModelListener.super.onError(errorContext);
    }
}
