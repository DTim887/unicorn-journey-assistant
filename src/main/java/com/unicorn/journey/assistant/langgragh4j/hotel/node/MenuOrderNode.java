package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.MenuOrderAgent;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 点餐服务节点
 */
@Slf4j
@RequiredArgsConstructor
public class MenuOrderNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建点餐节点
     */
    public AsyncNodeAction<org.bsc.langgraph4j.prebuilt.MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 点餐服务节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null || !"MENU_ORDER".equals(currentTask.getTaskType())) {
                log.warn("当前任务不是点餐任务");
                return HotelAssistantContext.saveContext(context);
            }

            try {
                // 从任务参数中获取信息
                Map<String, Object> params = currentTask.getParams();
                String mealType = (String) params.getOrDefault("mealType", "晚餐");
                Integer peopleCount = params.get("peopleCount") != null ? 
                        Integer.parseInt(params.get("peopleCount").toString()) : 2;
                String dietaryRestrictions = (String) params.getOrDefault("dietaryRestrictions", "无");

                log.info("开始生成菜单: mealType={}, peopleCount={}, dietaryRestrictions={}", 
                        mealType, peopleCount, dietaryRestrictions);

                // 调用 AI Agent 生成菜单
                MenuOrderAgent agent = agentFactory.getMenuOrderAgent(context.getSessionId());
                String menuContent = agent.generateMenu(
                        mealType,
                        peopleCount,
                        dietaryRestrictions,
                        "请生成菜单推荐"
                );

                log.info("菜单生成完成，内容长度: {}", menuContent.length());

                // 发送 SSE 事件展示菜单
                HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, menuContent, "menu_order", "MENU");

                // 保存任务结果
                currentTask.setResult(menuContent);
                context.getTaskResults().put(currentTask.getTaskId(), menuContent);

                // 标记需要用户确认菜单
                context.setNeedConfirmation(true);
                context.setConfirmationType("MENU");
                context.setPausedAtNode("menu_order");
                currentTask.setStatus("WAITING_CONFIRMATION");
                context.setCurrentStep("请确认菜单是否满意");
                
                // 清除之前的确认结果，确保进入 ConfirmMenuNode 时为 null
                context.setConfirmationResult(null);

                log.info("菜单已生成，等待用户确认");

            } catch (Exception e) {
                log.error("生成菜单失败", e);
                currentTask.setStatus("FAILED");
                currentTask.setResult("生成菜单失败: " + e.getMessage());
                context.setCurrentStep("生成菜单失败");
            }

            return HotelAssistantContext.saveContext(context);
        });
    }
}
