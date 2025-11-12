package com.unicorn.journey.assistant.langgragh4j.agent;

import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 统筹节点 AI 代理
 * 用于智能判断和处理用户输入,协调整个工作流
 */
public interface OrchestratorAgent {

    /**
     * 智能判断缺少的信息并生成提示
     * 返回格式: "NEED_DATE|提示语" 或 "NEED_COUNT|提示语" 或 "COMPLETE"
     */
    @SystemMessage("""
        你是一个智能工作流助手,负责检查用户信息的完整性。
        
        上下文信息:{{context}}
        
        请按照以下规则判断:
        1. 如果游玩日期为空或null,返回: NEED_DATE|[友好的日期提示语]
        2. 如果游玩日期不为空但游玩人数为空或null或小于等于0,返回: NEED_COUNT|[友好的人数提示语]
        3. 如果日期和人数都完整,返回: COMPLETE
        
        提示语要求:
        - 简洁明了不超过20个字
        - 语气友好
        - 使用合适的emoji
        - 直接是提示内容,不要额外解释
        
        示例:
        - NEED_DATE|📅 请告诉我您的游玩日期哦~
        - NEED_COUNT|👥 请输入游玩人数(1-20人)
        - COMPLETE
        """)
    String checkAndPrompt(@V("context") ConfirmWorkflowContext context,
                          @UserMessage String userMessage);

    /**
     * 生成完成汇总信息
     */
    @SystemMessage("你是一个智能工作流助手,负责生成友好的完成汇总信息. 工作流已完成, 游玩日期 {{visitDate}}, 游玩人数 {{visitorCount}}人, 行程ID {{planId}}, 订单ID {{orderId}}. 要求使用友好的语气和emoji, 突出重要信息ID, 简洁明了100字以内, 表达感谢和祝福")
    String generateSummary(@V("visitDate") String visitDate,
                           @V("visitorCount") Integer visitorCount,
                           @V("planId") String planId,
                           @V("orderId") String orderId,
                           @UserMessage String userMessage);
}