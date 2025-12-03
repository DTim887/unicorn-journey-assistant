package com.unicorn.journey.assistant.hotel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.journey.assistant.hotel.entity.MenuItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务
 */
@Slf4j
@Service
public class MenuService {
    
    private final List<MenuItem> menuDatabase = new ArrayList<>();
    private final ObjectMapper objectMapper;
    
    public MenuService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initMenuData();
    }
    
    /**
     * 初始化菜单数据 - 从menu.json加载
     */
    private void initMenuData() {
        try {
            ClassPathResource resource = new ClassPathResource("menu.json");
            List<MenuItem> items = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<MenuItem>>() {}
            );
            menuDatabase.addAll(items);
            log.info("加载菜单数据成功，共 {} 道菜品", menuDatabase.size());
        } catch (IOException e) {
            log.error("加载菜单数据失败", e);
            // 如果加载失败，使用默认数据
            loadDefaultMenuData();
        }
    }
    
    /**
     * 加载默认菜单数据（备用）
     */
    private void loadDefaultMenuData() {
        log.warn("使用默认菜单数据");
        menuDatabase.add(MenuItem.builder()
                .menuId(1).name("清蒸鲈鱼").category("中式").flavors(Arrays.asList("清淡", "鲜"))
                .image("/images/menu/steamed_sea_bass.jpg")
                .price(68.0).description("新鲜鲈鱼清蒸，保留原味").build());
        menuDatabase.add(MenuItem.builder()
                .menuId(2).name("躁椒鱼头").category("中式").flavors(Arrays.asList("辣", "鲜"))
                .image("/images/menu/fish_head_with_chopped_chili.jpg")
                .price(68.0).description("湘菜经典，鱼肉鲜嫩").build());
        menuDatabase.add(MenuItem.builder()
                .menuId(3).name("蒜蓉西兰花").category("中式").flavors(Arrays.asList("清淡", "蒜香"))
                .image("/images/menu/garlic_broccoli.webp")
                .price(28.0).description("清爭健康的时蔬").build());
    }
    
    /**
     * 获取所有菜品（按ID排序）
     */
    public List<MenuItem> getAllMenuItems() {
        return menuDatabase.stream()
                .sorted(Comparator.comparing(MenuItem::getMenuId))
                .toList();
    }
    
    /**
     * 根据条件筛选菜品（按ID排序）
     */
    public List<MenuItem> filterMenuItems(String category, String flavors) {
        return menuDatabase.stream()
                .filter(item -> category == null || category.isEmpty() || item.getCategory().equals(category))
                .filter(item -> flavors == null || flavors.isEmpty() || 
                        (item.getFlavors() != null && item.getFlavors().contains(flavors)))
                .sorted(Comparator.comparing(MenuItem::getMenuId))
                .toList();
    }
    
    /**
     * 根据ID获取菜品
     */
    public MenuItem getMenuItemById(Integer menuId) {
        return menuDatabase.stream()
                .filter(item -> item.getMenuId().equals(menuId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据名称获取菜品
     */
    public MenuItem getMenuItemByName(String name) {
        return menuDatabase.stream()
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 计算多个菜品的总价
     * @param menuIds 菜品ID列表，多个ID用逗号分隔（例如："1,2,3"）
     * @return 总价
     */
    public double calculateTotalPrice(String menuIds) {
        if (menuIds == null || menuIds.trim().isEmpty()) {
            log.warn("菜品ID列表为空，返回总价0");
            return 0.0;
        }
        
        try {
            String[] ids = menuIds.split(",");
            double total = 0.0;
            
            for (String idStr : ids) {
                try {
                    Integer menuId = Integer.parseInt(idStr.trim());
                    MenuItem item = getMenuItemById(menuId);
                    if (item != null) {
                        total += item.getPrice();
                        log.debug("菜品ID: {}, 名称: {}, 价格: {}", menuId, item.getName(), item.getPrice());
                    } else {
                        log.warn("未找到菜品ID: {}", menuId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("无效的菜品ID: {}", idStr);
                }
            }
            
            log.info("计算总价：菜品ID列表={}, 总价={}", menuIds, total);
            return total;
        } catch (Exception e) {
            log.error("计算总价失败: {}", e.getMessage());
            return 0.0;
        }
    }
}
