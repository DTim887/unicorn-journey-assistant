package com.unicorn.journey.assistant.hotel.service;

import com.unicorn.journey.assistant.hotel.agent.HotelRouterAgent;
import com.unicorn.journey.assistant.hotel.agent.MOAgent;
import com.unicorn.journey.assistant.hotel.agent.WakeUpAgent;
import com.unicorn.journey.assistant.hotel.entity.WakeUpAssistance;
import com.unicorn.journey.assistant.hotel.enums.SseEventType;
import com.unicorn.journey.assistant.hotel.factory.HotelAgentFactory;
import com.unicorn.journey.assistant.hotel.entity.MenuItem;
import com.unicorn.journey.assistant.hotel.entity.MenuOrder;
import com.unicorn.journey.assistant.hotel.entity.SessionContext;
import com.unicorn.journey.assistant.hotel.utils.SseEventSender;
import com.unicorn.journey.assistant.service.STTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 酒店助手服务
 */
@Slf4j
@Service
public class HotelAssistantService {

    private final HotelAgentFactory hotelAgentFactory;
    private final MenuService menuService;
    private final SseEventSender sseEventSender;
    private final STTService sttService;
    private final WakeUpSchedulerService wakeUpSchedulerService;

    // Agent实例
    private HotelRouterAgent hotelRouterAgent;
    private MOAgent moAgent;
    private WakeUpAgent wakeUpAgent;

    // 存储会话上下文（包含菜单、业务类型、SSE等所有会话数据）
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    // 存储订单
    private final Map<String, MenuOrder> orders = new ConcurrentHashMap<>();

    // 存储叫醒服务 key:wakeUpId
    private final Map<String, WakeUpAssistance> wakeUpAssistanceMap = new ConcurrentHashMap<>();

    public HotelAssistantService(HotelAgentFactory hotelAgentFactory,
                                  MenuService menuService,
                                  SseEventSender sseEventSender,
                                  STTService sttService,
                                  WakeUpSchedulerService wakeUpSchedulerService) {
        this.hotelAgentFactory = hotelAgentFactory;
        this.menuService = menuService;
        this.sseEventSender = sseEventSender;
        this.sttService = sttService;
        this.wakeUpSchedulerService = wakeUpSchedulerService;
    }

    /**
     * 获取路由Agent
     */
    private HotelRouterAgent getRouterAgent() {
        if (hotelRouterAgent == null) {
            hotelRouterAgent = hotelAgentFactory.createRouterAgent();
        }
        return hotelRouterAgent;
    }

    /**
     * 获取点餐Agent
     */
    private MOAgent getMOAgent() {
        if (moAgent == null) {
            moAgent = hotelAgentFactory.createMOAgent();
        }
        return moAgent;
    }

    /**
     * 获取叫醒服务Agent
     */
    private WakeUpAgent getWakeUpAgent() {
        if (wakeUpAgent == null) {
            wakeUpAgent = hotelAgentFactory.createWakeUpAgent();
        }
        return wakeUpAgent;
    }

    /**
     * 创建新会话并返回会话上下文
     */
    private SessionContext createSession(String sessionId, String userId) {
        // 创建SSE，60分钟超时
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.info("[SESSION] sessionId: {} 超时，移除SSE连接", sessionId);
            sessionContexts.remove(sessionId);
        });

        emitter.onCompletion(() -> {
            log.info("[SESSION] sessionId: {} 完成，移除SSE连接", sessionId);
            sessionContexts.remove(sessionId);
        });

        emitter.onError((e) -> {
            log.error("[SESSION] sessionId: {} 错误，移除SSE连接: {}", sessionId, e.getMessage());
            sessionContexts.remove(sessionId);
        });

        // 创建并存储会话上下文
        SessionContext context = SessionContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .sseEmitter(emitter)
                .lastDataType("")
                .build();

        sessionContexts.put(sessionId, context);
        log.info("[SESSION] 创建新会话: sessionId={}", sessionId);

        return context;
    }

    /**
     * 处理用户消息，返回SSE对象
     * 如果sessionId为空或不存在，则创建新会话；否则使用已有会话
     */
    public SseEmitter chat(String userId, String userMessage, String sessionId, boolean enableVoiceOutput) {
        SessionContext context;
        boolean isNewSession = false;

        // 如果sessionId为空或不存在，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session_" + userId + "_" + System.currentTimeMillis();
            context = createSession(sessionId, userId);
            isNewSession = true;
            log.info("[CHAT] 创建新会话: sessionId={}, userId={}", sessionId, userId);
        } else {
            context = sessionContexts.get(sessionId);
            if (context == null) {
                // 会话已过期，创建新会话
                sessionId = "session_" + userId + "_" + System.currentTimeMillis();
                context = createSession(sessionId, userId);
                isNewSession = true;
                log.info("[CHAT] 会话已过期，创建新会话: sessionId={}, userId={}", sessionId, userId);
            } else {
                log.info("[CHAT] 使用已有会话: sessionId={}, userId={}", sessionId, userId);
            }
        }

        // 如果是新会话，先发送session_created事件
        final String finalSessionId = sessionId;
        final boolean finalIsNewSession = isNewSession;
        final SessionContext finalContext = context;
        final SseEmitter finalEmitter = context.getSseEmitter();

        Thread.startVirtualThread(() -> {
            try {
                // 如果是新会话，先发送sessionId
                if (finalIsNewSession) {
                    try {
                        Map<String, Object> sessionData = Map.of(
                            "sessionId", finalSessionId,
                            "userId", userId,
                            "createTime", System.currentTimeMillis()
                        );
                        finalEmitter.send(SseEmitter.event()
                                .name(SseEventType.SESSION_CREATED.getEventName())
                                .data(sessionData));
                        log.info("[SESSION] 发送sessionId: {}", finalSessionId);
                    } catch (Exception e) {
                        log.error("[SESSION] 发送sessionId失败: {}", e.getMessage(), e);
                    }
                }

                String memoryId = "hotel_" + userId;

                // 先通过路由Agent判断意图
                log.info("[SSE] sessionId: {}, 用户消息: {}", finalSessionId, userMessage);

                // 获取业务上下文（使用SessionContext中的方法）
                String businessContext = finalContext.getBusinessContextString();
                if (!businessContext.isEmpty()) {
                    if (finalContext.isInMenuProcess()) {
                        log.info("[ROUTER] sessionId: {}, 检测到用户处于点餐流程", finalSessionId);
                    } else if (finalContext.isInWakeUpProcess()) {
                        log.info("[ROUTER] sessionId: {}, 检测到用户处于叫醒流程", finalSessionId);
                    }
                }

                String routeDecision = getRouterAgent().routeToAgentWithContext(memoryId + "_router", userMessage, businessContext).trim();
                log.info("[SSE] sessionId: {}, 路由决策: {}", finalSessionId, routeDecision);

                String response;
                String dataType = "NONE";
                Object structuredData = null;

                // 根据路由决策调用相应的Agent，获取响应
                if (routeDecision.contains("MO_AGENT")) {
                    // 调用点餐Agent
                    String menuList = formatMenuList();
                    response = getMOAgent().chat(memoryId + "_mo", userMessage, menuList);

                    // 检查响应中的标记，确定需要发送的数据类型
                    if (response.contains("[SHOW_MENU]")) {
                        // 展示菜单列表（初始菜单或筛选后）
                        dataType = "MENU";
                        structuredData = extractMenuFromResponse(response);
                        response = response.replace("[SHOW_MENU]", "").trim();
                    } else if (response.contains("[CONFIRM_MENU]")) {
                        // 确认菜单（用户已选择菜品，等待确认）
                        dataType = "CONFIRM";
                        structuredData = extractSelectedMenuFromResponse(response, finalContext);
                        response = response.replace("[CONFIRM_MENU]", "").trim();
                    } else if (response.contains("[GENERATE_ORDER]")) {
                        // 生成订单（用户最终确认）
                        dataType = "ORDER";
                        structuredData = generateOrder(userId, finalContext);
                        response = response.replace("[GENERATE_ORDER]", "").trim();
                    }

                } else if (routeDecision.contains("WAKEUP_AGENT")) {
                    // 调用叫醒服务Agent
                    response = getWakeUpAgent().chat(memoryId + "_wakeup", userMessage);

                    // 检查响应中的标记
                    if (response.contains("[REQUEST_TIME_INPUT]")) {
                        // 请求用户输入叫醒时间
                        dataType = "TIME_INPUT";
                        response = response.replace("[REQUEST_TIME_INPUT]", "").trim();
                    } else if (response.contains("[GENERATE_WAKEUP]")) {
                        // 生成叫醒服务
                        dataType = "WAKEUP";
                        WakeUpAssistance wakeUpAssistance = generateWakeUpAssistance(userId, response, finalSessionId, finalEmitter);
                        structuredData = wakeUpAssistance;
                        response = response.replace("[GENERATE_WAKEUP]", "").trim();

                        // 生成叫醒语音（用于定时播放）
                        try {
                            String wakeUpVoiceText = generateWakeUpVoiceText(wakeUpAssistance);
                            log.info("[WAKEUP-VOICE] 开始生成叫醒语音: {}", wakeUpVoiceText.substring(0, Math.min(50, wakeUpVoiceText.length())));
                            String voicePath = sttService.textToSpeechAndSave(wakeUpVoiceText);
                            log.info("[WAKEUP-VOICE] 叫醒语音生成成功: {}", voicePath);

                            // 存储语音路径
                            wakeUpAssistance.setVoicePath(voicePath);

                        } catch (Exception e) {
                            log.error("[WAKEUP-VOICE] 叫醒语音生成失败: {}", e.getMessage(), e);
                            // 语音生成失败不影响主流程
                        }
                    }

                } else {
                    // 路由Agent直接回复
                    response = "你好！我是酒店助手，可以帮您：\n1. 点餐服务\n2. 设置叫醒服务\n\n请问需要什么帮助？";
                }

                // 发送文本响应（同步返回对话内容）
                log.info("[SSE] sessionId: {}, 发送对话内容: {}", finalSessionId, response);
                sseEventSender.sendEvent(finalSessionId, finalEmitter, SseEventType.MESSAGE.getEventName(), Map.of("content", response));

                // 如果启用语音输出，将文本转换为语音并发送
                if (enableVoiceOutput && !response.isEmpty()) {
                    try {
                        log.info("[VOICE] 开始生成语音: {}", response.substring(0, Math.min(50, response.length())));
                        String audioPath = sttService.textToSpeechAndSave(response);
                        log.info("[VOICE] 语音生成成功: {}", audioPath);

                        // 发送语音消息
                        sseEventSender.sendEvent(finalSessionId, finalEmitter,
                            SseEventType.VOICE.getEventName(),
                            Map.of(
                                "audioPath", audioPath,
                                "text", response,
                                "type", "assistant"
                            ));
                    } catch (Exception e) {
                        log.error("[VOICE] 语音生成失败: {}", e.getMessage(), e);
                        // 语音生成失败不影响主流程，继续处理
                    }
                }

                // 如果有结构化数据，通过SSE异步发送
                if (structuredData != null) {
                    // 保存当前会话的最后业务数据类型，便下次路由判断
                    if (dataType.equals("MENU") || dataType.equals("CONFIRM")) {
                        finalContext.updateLastDataType("MENU");
                    } else if (dataType.equals("WAKEUP")) {
                        finalContext.updateLastDataType("WAKEUP");
                    } else if (dataType.equals("ORDER")) {
                        // 订单完成后，清除最后的数据类型，以便下一次模拘新业务
                        finalContext.updateLastDataType("");
                    }

                    log.info("[SSE] sessionId: {}, 发送结构化数据，类型: {}", finalSessionId, dataType);
                    switch (dataType) {
                        case "MENU" -> {
                            // 展示菜单列表（初始或筛选后）
                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.STRUCTURED_DATA.getEventName(),
                                Map.of("type", "MENU", "data", structuredData));
                        }
                        case "CONFIRM" -> {
                            // 确认菜单（等待用户确认）
                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.CONFIRM_MENU.getEventName(),
                                Map.of("type", "CONFIRM", "data", structuredData));
                        }
                        case "ORDER" -> {
                            // 正式订单
                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.STRUCTURED_DATA.getEventName(),
                                Map.of("type", "ORDER", "data", structuredData));
                        }
                        case "TIME_INPUT" -> {
                            // 请求用户输入时间
                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.STRUCTURED_DATA.getEventName(),
                                Map.of("type", "TIME_INPUT", "data", Map.of("message", "请选择叫醒时间")));
                        }
                        case "WAKEUP" -> {
                            // 叫醒服务
                            sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                SseEventType.STRUCTURED_DATA.getEventName(),
                                Map.of("type", "WAKE_UP", "data", structuredData));

                            // 如果有语音路径，也发送语音事件
                            if (structuredData instanceof WakeUpAssistance wakeUp) {
                                String remark = wakeUp.getRemark();
                                if (remark != null && remark.startsWith("audioPath:")) {
                                    String audioPath = remark.substring("audioPath:".length());
                                    log.info("[WAKEUP-VOICE] 发送叫醒服务语音: {}", audioPath);

                                    // 生成确认文案用于语音消息显示
                                    String voiceText = generateWakeUpText(wakeUp);

                                    sseEventSender.sendEvent(finalSessionId, finalEmitter,
                                        SseEventType.VOICE.getEventName(),
                                        Map.of(
                                            "audioPath", audioPath,
                                            "text", voiceText,
                                            "type", "wakeup_confirmation",
                                                "timeInterval", "100"
                                        ));

                                    // 清理remark中的语音路径，恢复为原始备注
                                    wakeUp.setRemark("酒店叫醒服务");
                                }
                            }
                        }
                    }
                }

                log.info("[SSE] sessionId: {} 消息处理完成，保持连接", finalSessionId);

            } catch (Exception e) {
                log.error("[SSE] sessionId: {}, 处理消息失败: {}", finalSessionId, e.getMessage(), e);
                sseEventSender.sendError(finalSessionId, finalEmitter, e.getMessage());
            }
        });

        return finalEmitter;
    }

    /**
     * 格式化菜单列表为字符串
     */
    private String formatMenuList() {
        List<MenuItem> allItems = menuService.getAllMenuItems();
        StringBuilder sb = new StringBuilder();
        sb.append("所有可用菜品：\n\n");

        // 按类型分组
        Map<String, List<MenuItem>> groupedByCategory = allItems.stream()
            .collect(java.util.stream.Collectors.groupingBy(MenuItem::getCategory));

        groupedByCategory.forEach((category, items) -> {
            sb.append("**").append(category).append("菜品**\n");
            for (MenuItem item : items) {
                sb.append(String.format("%d. %s - %.0f元（%s）\n",
                        item.getMenuId(), item.getName(), item.getPrice(),
                        item.getFlavors()));
            }
            sb.append("\n");
        });

        return sb.toString();
    }

    /**
     * 从响应中提取菜单
     */
    private List<MenuItem> extractMenuFromResponse(String response) {
        List<MenuItem> menuItems = new ArrayList<>();

        log.info("[MENU] 开始提取菜单，响应内容: {}", response);

        // 优化正则，兼容多种格式：
        // 1. 数字. 菜品名 - 价格元
        // 2. **数字. 菜品名 - 价格元**
        // 3. 数字. **菜品名 - 价格元**
        // 4. - 菜品名 - 价格元（不带序号）
        Pattern pattern = Pattern.compile("(?:^|\n)\\s*(?:\\d+\\.)?\\s*\\*{0,2}\\s*([^−\\-*\n（(]+?)\\s*\\*{0,2}\\s*[−\\-]\\s*([\\d.]+)元", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        int matchCount = 0;
        while (matcher.find()) {
            String dishName = matcher.group(1).trim();
            // 过滤掉不是菜品名的内容
            if (dishName.isEmpty() || dishName.equals("-")) {
                continue;
            }

            log.info("[MENU] 匹配到菜品: {}", dishName);

            MenuItem item = menuService.getMenuItemByName(dishName);
            if (item != null) {
                menuItems.add(item);
                matchCount++;
                log.info("[MENU] 成功添加菜品: {}", item.getName());
            } else {
                log.warn("[MENU] 菜品不存在: {}", dishName);
            }
        }

        log.info("[MENU] 提取完成，共匹配 {} 个菜品", matchCount);

        return menuItems;
    }

    /**
     * 从确认响应中提取用户已选择的菜品
     * 用于生成确认菜单
     */
    private List<MenuItem> extractSelectedMenuFromResponse(String response, SessionContext context) {
        List<MenuItem> selectedItems = new ArrayList<>();

        log.info("[CONFIRM] 开始提取已选菜品，响应内容: {}", response);

        // 提取确认菜单中的菜品
        // 支持两种格式：
        // 1. 数字. 菜品名 - 价格元（带序号）
        // 2. - 菜品名 - 价格元（无序号，用-开头）
        Pattern pattern = Pattern.compile("(?:^|\n)\\s*(?:\\d+\\.)?\\s*([^−\\-*\n（(]+?)\\s*[−\\-]\\s*([\\d.]+)元", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);

        int matchCount = 0;
        while (matcher.find()) {
            String dishName = matcher.group(1).trim();
            // 过滤掉不是菜品名的内容（如"- "开头的列表项标记）
            if (dishName.isEmpty() || dishName.equals("-")) {
                continue;
            }

            log.info("[CONFIRM] 匹配到已选菜品: {}", dishName);

            MenuItem item = menuService.getMenuItemByName(dishName);
            if (item != null) {
                selectedItems.add(item);
                matchCount++;
                log.info("[CONFIRM] 成功添加已选菜品: {}", item.getName());
            } else {
                log.warn("[CONFIRM] 菜品不存在: {}", dishName);
            }
        }

        log.info("[CONFIRM] 提取完成，共匹配 {} 个已选菜品", matchCount);

        // 存储当前会话的已选菜品到SessionContext中
        context.setSelectedMenuItems(selectedItems);

        return selectedItems;
    }

    /**
     * 生成订单
     */
    private MenuOrder generateOrder(String userId, SessionContext context) {
        List<MenuItem> items = context.getSelectedMenuItems() != null ? context.getSelectedMenuItems() : new ArrayList<>();

        double totalPrice = items.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum();

        MenuOrder order = MenuOrder.builder()
                .orderId("ORDER_" + System.currentTimeMillis())
                .userId(userId)
                .items(items)
                .totalPrice(totalPrice)
                .createTime(LocalDateTime.now())
                .status("CONFIRMED")
                .build();

        orders.put(order.getOrderId(), order);
        log.info("生成订单: {}", order.getOrderId());

        return order;
    }

    /**
     * 生成叫醒服务
     */
    private WakeUpAssistance generateWakeUpAssistance(String userId, String response, String sessionId, SseEmitter emitter) {
        log.info("[WAKEUP] 开始生成叫醒服务，响应内容: {}", response);

        // 提取叫醒时间
        LocalDateTime wakeUpTime = extractTimeFromResponse(response);

        if (wakeUpTime == null) {
            log.error("[WAKEUP] 无法从响应中提取时间");
            wakeUpTime = LocalDateTime.now().plusDays(1).withHour(7).withMinute(0).withSecond(0).withNano(0);
        }

        WakeUpAssistance wakeUpAssistance = WakeUpAssistance.builder()
                .wakeUpId("WAKEUP_" + System.currentTimeMillis())
                .userId(userId)
                .sessionId(sessionId)  // 存储sessionId，用于后续发送SSE事件
                .wakeUpTime(wakeUpTime)
                .createTime(LocalDateTime.now())
                .status("PENDING")
                .remark("酒店叫醒服务")
                .build();

        wakeUpAssistanceMap.put(wakeUpAssistance.getWakeUpId(), wakeUpAssistance);
        log.info("[WAKEUP] 生成叫醒服务: {}, 时间: {}, sessionId: {}",
                wakeUpAssistance.getWakeUpId(), wakeUpTime, sessionId);

        // 调度叫醒任务
        wakeUpSchedulerService.scheduleWakeUp(wakeUpAssistance, emitter);

        return wakeUpAssistance;
    }

    /**
     * 生成叫醒语音文本（用于定时播放）
     */
    private String generateWakeUpVoiceText(WakeUpAssistance wakeUp) {
        LocalDateTime wakeUpTime = wakeUp.getWakeUpTime();

        // 格式化时间
        int hour = wakeUpTime.getHour();
        int minute = wakeUpTime.getMinute();

        // 生成友好的叫醒语音
        String timeGreeting;
        if (hour >= 5 && hour < 9) {
            timeGreeting = "早上好";
        } else if (hour >= 9 && hour < 12) {
            timeGreeting = "上午好";
        } else if (hour >= 12 && hour < 14) {
            timeGreeting = "中午好";
        } else if (hour >= 14 && hour < 18) {
            timeGreeting = "下午好";
        } else {
            timeGreeting = "晚上好";
        }

        String minuteText = minute == 0 ? "" : String.format("%d分", minute);

        return String.format(
            "尊敬的客人，%s！\n\n" +
            "现在是%d点%s，该起床啦！\n\n" +
            "祇您今天有个美好的一天！",
            timeGreeting,
            hour,
            minuteText
        );
    }

    /**
     * 生成叫醒服务确认文案
     */
    private String generateWakeUpConfirmationText(WakeUpAssistance wakeUp) {
        LocalDateTime wakeUpTime = wakeUp.getWakeUpTime();

        // 格式化时间
        String timeStr = String.format("%d月%d日 %02d:%02d",
                wakeUpTime.getMonthValue(),
                wakeUpTime.getDayOfMonth(),
                wakeUpTime.getHour(),
                wakeUpTime.getMinute());

        // 判断是明天还是后天
        LocalDateTime now = LocalDateTime.now();
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), wakeUpTime.toLocalDate());
        String dayText = "";
        if (daysDiff == 1) {
            dayText = "明天";
        } else if (daysDiff == 2) {
            dayText = "后天";
        }

        // 生成专业的确认文案
        return String.format(
            "尊敬的客人，您好！\n\n" +
            "您的叫醒服务已成功设置。\n" +
            "叫醒时间：%s%s\n" +
            "服务编号：%s\n\n" +
            "我们将在指定时间准时为您提供叫醒服务。祝您住店愉快，睡眠充足！",
            dayText.isEmpty() ? "" : dayText + " ",
            timeStr,
            wakeUp.getWakeUpId()
        );
    }

    /**
     * 生成叫醒服务文案
     */
    private String generateWakeUpText(WakeUpAssistance wakeUp) {
        LocalDateTime wakeUpTime = wakeUp.getWakeUpTime();

        // 格式化时间
        String timeStr = String.format("%d月%d日 %02d:%02d",
                wakeUpTime.getMonthValue(),
                wakeUpTime.getDayOfMonth(),
                wakeUpTime.getHour(),
                wakeUpTime.getMinute());

        // 生成专业的确认文案
        return String.format(
                "尊敬的客人，您好！\n\n" +
                        "您的叫醒服务已成功执行。\n" +
                        "叫醒时间：%s\n" +
                        "服务编号：%s\n\n" +
                        "祝您住店愉快，睡眠充足！",
                timeStr,
                wakeUp.getWakeUpId()
        );
    }

    /**
     * 从响应中提取时间
     * 支持格式：
     * - "明天早上7点" / "明天上午7点"
     * - "07:00" / "7:00"
     * - "早上7点" / "上午7点半"
     */
    private LocalDateTime extractTimeFromResponse(String response) {
        LocalDateTime now = LocalDateTime.now();

        // 尝试匹配 HH:mm 格式
        Pattern timePattern = Pattern.compile("(\\d{1,2})[:：](\\d{2})");
        Matcher timeMatcher = timePattern.matcher(response);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = Integer.parseInt(timeMatcher.group(2));

            // 判断是今天还是明天
            LocalDateTime result = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            if (result.isBefore(now)) {
                result = result.plusDays(1);
            }

            log.info("[WAKEUP] 提取时间成功: {}", result);
            return result;
        }

        // 尝试匹配中文时间表达（早上X点、上午X点半）
        Pattern chinesePattern = Pattern.compile("(早上|上午|中午|下午|晚上).*?(\\d{1,2})点(半|\\d{2}分)?");
        Matcher chineseMatcher = chinesePattern.matcher(response);
        if (chineseMatcher.find()) {
            String period = chineseMatcher.group(1);
            int hour = Integer.parseInt(chineseMatcher.group(2));
            String minutePart = chineseMatcher.group(3);

            // 转换为24小时制
            if (("下午".equals(period) || "晚上".equals(period)) && hour < 12) {
                hour += 12;
            }

            int minute = 0;
            if ("半".equals(minutePart)) {
                minute = 30;
            } else if (minutePart != null && minutePart.endsWith("分")) {
                minute = Integer.parseInt(minutePart.replace("分", ""));
            }

            LocalDateTime result = now.plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            log.info("[WAKEUP] 提取时间成功: {}", result);
            return result;
        }

        log.warn("[WAKEUP] 无法提取时间，使用默认值（明天7:00）");
        return null;
    }

    /**
     * 获取订单详情
     */
    public MenuOrder getOrder(String orderId) {
        return orders.get(orderId);
    }

    /**
     * 获取用户所有订单
     */
    public List<MenuOrder> getUserOrders(String userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .toList();
    }

    /**
     * 获取叫醒服务详情
     */
    public WakeUpAssistance getWakeUpAssistance(String wakeUpId) {
        return wakeUpAssistanceMap.get(wakeUpId);
    }

    /**
     * 获取用户所有叫醒服务
     */
    public List<WakeUpAssistance> getUserWakeUpAssistanceMap(String userId) {
        return wakeUpAssistanceMap.values().stream()
                .filter(service -> service.getUserId().equals(userId))
                .toList();
    }

    /**
     * 删除叫醒服务
     * @param wakeUpId 叫醒服务ID
     * @return 是否删除成功
     */
    public boolean deleteWakeUpService(String wakeUpId) {
        WakeUpAssistance removed = wakeUpAssistanceMap.remove(wakeUpId);
        if (removed != null) {
            // 取消定时任务
            wakeUpSchedulerService.cancelWakeUp(wakeUpId);

            log.info("[WAKEUP] 删除叫醒服务成功: {}, 时间: {}", wakeUpId, removed.getWakeUpTime());
            return true;
        } else {
            log.warn("[WAKEUP] 叫醒服务不存在: {}", wakeUpId);
            return false;
        }
    }

    /**
     * 清除会话数据
     */
    public void clearSession(String sessionId) {
        SessionContext context = sessionContexts.remove(sessionId);
        if (context != null) {
            context.clear();
            log.info("[SESSION] 清除会话数据: sessionId={}", sessionId);
        }
    }
}
