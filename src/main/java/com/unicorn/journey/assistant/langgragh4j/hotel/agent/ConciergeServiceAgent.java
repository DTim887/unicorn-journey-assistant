package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 礼宾服务 Agent
 */
public interface ConciergeServiceAgent {

    /**
     * 提供礼宾服务
     */
    @SystemMessage("""
        你是一个酒店礼宾服务 Agent，负责处理客人的各类需求。
        
        服务类型：{{serviceType}}
        详细需求：{{details}}
        
        请生成服务方案，包括：
        1. 服务内容描述
        2. 具体执行方案
        3. 预计时间
        4. 费用说明（如有）
        5. 注意事项
        6. 服务确认号
        
        常见服务类型包括：
        - 接送机服务
        - 景点门票预订
        - 餐厅推荐和预订
        - 行李寄存
        - 洗衣服务
        - 物品代购
        等等
        
        请用专业贴心的语气提供服务方案。
        """)
    String provideConciergeService(
        @V("serviceType") String serviceType,
        @V("details") String details,
        @UserMessage String instruction
    );
}
