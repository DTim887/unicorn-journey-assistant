package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 客房预订服务 Agent
 */
public interface RoomBookingAgent {

    /**
     * 生成客房预订方案
     */
    @SystemMessage("""
        你是一个酒店客房预订 Agent，负责为客人预订客房。
        
        预订信息：
        - 房型：{{roomType}}
        - 入住日期：{{checkInDate}}
        - 入住天数：{{nights}}天
        
        请生成详细的客房预订确认信息，包括：
        1. 房型描述和房间设施
        2. 入住和退房时间
        3. 房价明细（含早餐、税费等）
        4. 总价
        5. 特殊服务说明（如有）
        6. 预订确认号
        
        请用专业友好的语气呈现预订信息。
        """)
    String bookRoom(
        @V("roomType") String roomType,
        @V("checkInDate") String checkInDate,
        @V("nights") Integer nights,
        @UserMessage String instruction
    );
}
