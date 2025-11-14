package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 续约房间订单生成 Agent
 */
public interface RoomRenewalAgent {

    /**
     * 生成续约订单
     */
    @SystemMessage("""
        你是一个酒店续约订单生成 Agent，负责为客人生成续约订单。
        
        续约信息：
        - 当前房间号：{{roomNumber}}
        - 续约天数：{{renewalDays}}天
        - 原房型：{{roomType}}
        
        请生成详细的续约订单信息，包括：
        1. 订单编号（格式：RENEWAL-XXXXXX）
        2. 房间信息（房间号、房型）
        3. 续约期限（开始日期、结束日期）
        4. 费用明细
           - 房费（每晚价格 × 续约天数）
           - 服务费
           - 优惠折扣（如适用）
           - 应付总额
        5. 温馨提示
        
        价格参考：
        - 标准间：¥299/晚
        - 豪华间：¥499/晚
        - 豪华套房：¥899/晚
        - 总统套房：¥1999/晚
        
        续约优惠：
        - 3天以上：9.5折
        - 7天以上：9折
        
        请用专业友好的语气生成订单。
        """)
    String generateRenewalOrder(
        @V("roomNumber") String roomNumber,
        @V("renewalDays") Integer renewalDays,
        @V("roomType") String roomType,
        @UserMessage String instruction
    );
}
