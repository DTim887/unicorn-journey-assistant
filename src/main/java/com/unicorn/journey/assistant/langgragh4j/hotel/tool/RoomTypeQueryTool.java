package com.unicorn.journey.assistant.langgragh4j.hotel.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 房型查询工具
 * 用于查询可用的房型和库存情况
 */
@Slf4j
@Component
public class RoomTypeQueryTool {

    /**
     * 查询当前可用的房型和库存
     * 
     * @return 房型和库存信息的格式化字符串
     */
    @Tool("查询酒店当前可用的房型和库存情况，返回所有可预订的房型列表")
    public String queryAvailableRoomTypes() {
        log.info("查询可用房型和库存");
        
        // 模拟查询数据库，获取房型库存
        Map<String, RoomTypeInfo> roomTypes = getRoomTypeInventory();
        
        // 格式化输出
        StringBuilder result = new StringBuilder();
        result.append("🏨 当前可预订房型：\n\n");
        
        int index = 1;
        for (Map.Entry<String, RoomTypeInfo> entry : roomTypes.entrySet()) {
            RoomTypeInfo info = entry.getValue();
            result.append(String.format("%d. **%s**\n", index++, info.name));
            result.append(String.format("   价格：¥%d/晚\n", info.price));
            result.append(String.format("   面积：%dm²\n", info.area));
            result.append(String.format("   可住人数：%d人\n", info.capacity));
            result.append(String.format("   剩余房间：%d间\n", info.available));
            result.append(String.format("   描述：%s\n", info.description));
            result.append("\n");
        }
        
        result.append("💡 请从上述房型中选择您想要预订的房型");
        
        String formattedResult = result.toString();
        log.info("房型查询结果: {}", formattedResult);
        
        return formattedResult;
    }
    
    /**
     * 模拟数据库查询，获取房型库存信息
     */
    private Map<String, RoomTypeInfo> getRoomTypeInventory() {
        Map<String, RoomTypeInfo> inventory = new HashMap<>();
        
        inventory.put("standard", new RoomTypeInfo(
            "标准间",
            298,
            25,
            2,
            15,
            "舒适经济，配备基础设施，适合商务出行"
        ));
        
        inventory.put("deluxe", new RoomTypeInfo(
            "豪华间",
            498,
            35,
            2,
            8,
            "宽敞明亮，高级装修，配备智能家居系统"
        ));
        
        inventory.put("suite", new RoomTypeInfo(
            "豪华套房",
            898,
            55,
            3,
            5,
            "独立客厅卧室，配备浴缸和观景阳台"
        ));
        
        inventory.put("presidential", new RoomTypeInfo(
            "总统套房",
            1998,
            120,
            4,
            2,
            "顶级奢华，360度全景落地窗，私人管家服务"
        ));
        
        return inventory;
    }
    
    /**
     * 房型信息实体
     */
    private static class RoomTypeInfo {
        String name;        // 房型名称
        int price;          // 价格/晚
        int area;           // 面积(平方米)
        int capacity;       // 可住人数
        int available;      // 剩余数量
        String description; // 描述
        
        RoomTypeInfo(String name, int price, int area, int capacity, int available, String description) {
            this.name = name;
            this.price = price;
            this.area = area;
            this.capacity = capacity;
            this.available = available;
            this.description = description;
        }
    }
}
