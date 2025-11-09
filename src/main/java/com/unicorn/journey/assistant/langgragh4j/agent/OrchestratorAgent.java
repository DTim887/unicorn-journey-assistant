package com.unicorn.journey.assistant.langgragh4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 统筹节点 AI 代理
 * 用于智能判断和处理用户输入，协调整个工作流
 */
public interface OrchestratorAgent {

    /**
     * 智能判断缺少的信息
     */
    @SystemMessage("""
            你是一个智能工作流助手，负责检查用户信息的完整性。
            
            请检查以下信息：
            - 游玩日期：{{visitDate}}
            - 游玩人数：{{visitorCount}}
            
            如果缺少信息，请生成友好的提示语。
            如果信息完整，请回复"信息完整"。
            
            要求：
            1. 简洁明了，不超过20个字
            2. 语气友好，使用 emoji
            3. 直接输出提示语，不要额外解释
            """)
    String checkInput(@V("visitDate") String visitDate, 
                     @V("visitorCount") Integer visitorCount,
                     @UserMessage String userMessage);

    /**
     * 生成完成汇总信息
     */
    @SystemMessage("""
            你是一个智能工作流助手，负责生成友好的完成汇总信息。
            
            工作流已完成，请根据以下信息生成汇总：
            - 游玩日期：{{visitDate}}
            - 游玩人数：{{visitorCount}}人
            - 行程ID：{{planId}}
            - 订单ID：{{orderId}}
            
            要求：
            1. 使用友好的语气和 emoji
            2. 突出重要信息（ID）
            3. 简洁明了，100字以内
            4. 表达感谢和祝福
            
            格式示例：
            🎉 您的迪士尼之旅已准备就绪！
            
            📅 游玩日期：2025-12-25
            👥 游玩人数：2人
            📋 行程ID：PLAN-XXXXXXXX
            🛍️ 订单ID：ORDER-XXXXXXXX
            
            感谢和祝福~~✨
            """)
    String generateSummary(@V("visitDate") String visitDate,
                          @V("visitorCount") Integer visitorCount,
                          @V("planId") String planId,
                          @V("orderId") String orderId,
                          @UserMessage String userMessage);
}

