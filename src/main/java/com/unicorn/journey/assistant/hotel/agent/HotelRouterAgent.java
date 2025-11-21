package com.unicorn.journey.assistant.hotel.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 路由Agent - 判断用户意图并路由到相应的子Agent
 */
public interface HotelRouterAgent {
    
    @SystemMessage("""
            你是一个酒店助手的路由代理。你的职责是判断用户的意图，并返回应该路由到哪个子代理。
            
            只有两种功能：
            1. 点餐服务 - 当用户想要订餐、点菜、推荐菜单时
            2. 叫醒服务 - 当用户需要叫醒服务、设置闹钟、预约叫醒时
            
            你要根据用户输入的信息判断用户的意图：
            - 如果是点餐相关，只返回：MO_AGENT
            - 如果是叫醒服务相关，只返回：WAKEUP_AGENT
            - 如果无法判断或打招呼，只返回：ROUTER_AGENT
            
            示例：
            用户："我想点餐" -> 返回：MO_AGENT
            用户："帮我设置明天早7点的叫醒" -> 返回：WAKEUP_AGENT
            用户："我需要叫醒服务" -> 返回：WAKEUP_AGENT
            用户："你好" -> 返回：ROUTER_AGENT
            """)
    String routeToAgent(@MemoryId String memoryId, @UserMessage String userMessage);
}
