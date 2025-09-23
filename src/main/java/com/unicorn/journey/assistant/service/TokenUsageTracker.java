package com.unicorn.journey.assistant.service;

import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class TokenUsageTracker {
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);

    // 记录一次调用的 Token 使用
    public void recordTokenUsage(TokenUsage tokenUsage) {
        if (tokenUsage != null) {
            totalInputTokens.addAndGet(tokenUsage.inputTokenCount());
            totalOutputTokens.addAndGet(tokenUsage.outputTokenCount());
            totalTokens.addAndGet(tokenUsage.totalTokenCount());
        }
    }

    // 获取当前累计值
    public long getTotalInputTokens() {
        return totalInputTokens.get();
    }

    public long getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    public long getTotalTokens() {
        return totalTokens.get();
    }
}
