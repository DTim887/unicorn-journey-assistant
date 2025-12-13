package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * MO Agent - 点餐代理
 * 支持完整的点餐流程：展示菜单 -> 筛选 -> 选择 -> 确认 -> 生成订单
 */
public interface MOAgent {
    
    @SystemMessage(fromResource = "/prompt/mo-prompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage, @V("menuList") String menuList);
}
