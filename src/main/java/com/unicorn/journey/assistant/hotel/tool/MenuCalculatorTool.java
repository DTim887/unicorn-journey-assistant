package com.unicorn.journey.assistant.hotel.tool;

import com.unicorn.journey.assistant.hotel.service.MenuService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 菜单计算工具 - 用于 MOAgent 计算菜品总价
 */
@Slf4j
@Component
public class MenuCalculatorTool {
    
    private final MenuService menuService;
    
    public MenuCalculatorTool(MenuService menuService) {
        this.menuService = menuService;
    }
    
    /**
     * 计算所选菜品的总价
     * 
     * @param menuIds 菜品ID列表，多个ID用英文逗号分隔，例如："1,2,3" 或 "1,3,5,7"
     * @return 总价（元）
     */
    @Tool("计算所选菜品的总价。输入参数为菜品ID列表（用英文逗号分隔），返回总价。例如：输入 \"1,2,3\" 返回 184.0")
    public double calculateTotalPrice(String menuIds) {
        log.info("Tool调用 - 计算总价，菜品ID列表: {}", menuIds);
        double total = menuService.calculateTotalPrice(menuIds);
        log.info("Tool返回 - 总价: {}", total);
        return total;
    }
}
