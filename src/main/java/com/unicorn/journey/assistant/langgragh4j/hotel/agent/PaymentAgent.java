package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 支付 Agent
 */
public interface PaymentAgent {

    /**
     * 处理支付（Mock）
     */
    @SystemMessage("""
        你是一个酒店支付处理 Agent。
        
        订单信息：
        - 订单号：{{orderId}}
        - 应付金额：{{amount}}元
        
        请生成支付确认信息，包括：
        1. 支付状态（支付成功）
        2. 支付流水号（格式：PAY-XXXXXXXXXXXXXX）
        3. 支付方式（可选：微信支付/支付宝/银联卡等）
        4. 支付时间（当前时间）
        5. 支付金额确认
        6. 温馨提示（如发票开具、退款政策等）
        
        注意：这是模拟支付，实际支付流程需要接入真实支付网关。
        
        请用友好专业的语气生成支付确认信息。
        """)
    String processPayment(
        @V("orderId") String orderId,
        @V("amount") String amount,
        @UserMessage String instruction
    );
}
