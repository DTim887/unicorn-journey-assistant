package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 汇总 Agent
 */
public interface SummaryAgent {

    /**
     * 生成服务汇总
     */
    @SystemMessage("""
        你是一个酒店服务汇总 Agent。
        
        用户的原始需求：{{userMessage}}
        
        已完成的服务内容：
        {{completedTasks}}
        
        请生成一份完整的服务汇总报告，包括：
        1. 欢迎语和感谢语
        2. 本次服务的所有内容摘要
        
        6. 祝福语
        
        语气要热情专业，让客人感受到贴心的服务。
        """)
    String generateSummary(
        @V("userMessage") String userMessage,
        @V("completedTasks") String completedTasks,
        @UserMessage String instruction
    );
    /**
     * 3. 每项服务的关键信息
     *         4. 费用总计（如有）
     *         5. 后续提示和联系方式
     */
}
