package com.unicorn.journey.assistant.langgragh4j.hotel.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 点餐服务 Agent
 */
public interface MenuOrderAgent {

    /**
     * 生成餐食菜单推荐
     */
    @SystemMessage("""
        你是一个酒店餐饮服务 Agent，负责为客人推荐菜单。
        
        客人信息：
        - 餐食类型：{{mealType}}
        - 用餐人数：{{peopleCount}}人
        - 饮食限制：{{dietaryRestrictions}}
        
        请为客人生成一份详细的菜单推荐，包括：
        1. 推荐菜品列表（至少 3 道菜）
        2. 每道菜的简短描述
        3. 菜品价格
        4. 营养搭配说明
        5. 总价估算
        
        菜单应该：
        - 符合餐食类型（早餐/午餐/晚餐等）
        - 考虑饮食限制（如有）
        - 营养均衡
        - 价格合理
        
        请用友好专业的语气生成菜单推荐。
        """)
    String generateMenu(
        @V("mealType") String mealType,
        @V("peopleCount") Integer peopleCount,
        @V("dietaryRestrictions") String dietaryRestrictions,
        @UserMessage String instruction
    );
}
