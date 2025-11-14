package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelRouterAgent;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext.AgentTask;
import com.unicorn.journey.assistant.langgragh4j.hotel.tool.RoomTypeQueryTool;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 路由调度节点
 */
@Slf4j
@RequiredArgsConstructor
public class RouterNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;
    private final RoomTypeQueryTool roomTypeQueryTool;

    /**
     * 创建任务分析节点
     */
    public AsyncNodeAction<MessagesState<String>> createAnalyzeNode() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 任务分析节点");

            try {
                // 使用 AI 分析用户需求，生成任务列表
                HotelRouterAgent agent = agentFactory.getRouterAgent(context.getSessionId());
                String tasksJson = agent.analyzeTasks(context.getUserMessage(), "请分析用户需求并生成任务列表");
                
                log.info("AI 生成任务列表: {}", tasksJson);

                // 解析任务列表
                JSONArray taskArray = JSONUtil.parseArray(tasksJson);
                List<AgentTask> tasks = new ArrayList<>();
                
                for (int i = 0; i < taskArray.size(); i++) {
                    JSONObject taskObj = taskArray.getJSONObject(i);
                    
                    AgentTask task = AgentTask.builder()
                            .taskId("TASK_" + UUID.randomUUID().toString().substring(0, 8))
                            .taskType(taskObj.getStr("taskType"))
                            .description(taskObj.getStr("description"))
                            .status("PENDING")
                            .params(taskObj.getJSONObject("params") != null ? 
                                    taskObj.getJSONObject("params").toBean(HashMap.class) : new HashMap<>())
                            .build();
                    
                    tasks.add(task);
                    log.info("解析任务: {} - {}", task.getTaskType(), task.getDescription());
                }

                context.setTasks(tasks);
                context.setCurrentTaskIndex(0);
                context.setCurrentStep("任务分析完成，共 " + tasks.size() + " 个任务");

                // 发送 SSE 事件
                HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, 
                        "任务分析完成，准备执行 " + tasks.size() + " 个任务", "router");

            } catch (Exception e) {
                log.error("任务分析失败", e);
                context.setCurrentStep("任务分析失败: " + e.getMessage());
            }

            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 创建参数检查节点
     */
    public AsyncNodeAction<MessagesState<String>> createCheckParamsNode() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 参数检查节点");

            AgentTask currentTask = context.getCurrentTask();
            if (currentTask == null) {
                log.warn("没有待执行的任务");
                context.setCurrentStep("所有任务已完成");
                return HotelAssistantContext.saveContext(context);
            }

            try {
                // 使用 AI 检查参数完整性，并从用户输入中提取缺失的参数
                HotelRouterAgent agent = agentFactory.getRouterAgent(context.getSessionId());
                String paramsJson = JSONUtil.toJsonStr(currentTask.getParams());
                String userMessage = context.getUserMessage(); // 获取用户原始输入
                String conversationHistory = context.getConversationHistoryText(); // 获取对话历史
                
                log.info("检查参数 - 当前输入: {}, 历史记录: {}", userMessage, conversationHistory);
                
                String checkResult = agent.checkTaskParams(
                        currentTask.getTaskType(),
                        currentTask.getDescription(),
                        paramsJson,
                        userMessage != null ? userMessage : "",
                        conversationHistory != null ? conversationHistory : "",
                        "请检查任务参数并从用户所有历史输入中提取缺失的参数"
                );

                log.info("参数检查结果: {}", checkResult);

                if (checkResult.startsWith("QUERY_TOOL")) {
                    // AI 判断需要调用工具查询可选值
                    String[] parts = checkResult.split("\\|", 2);
                    if (parts.length > 1) {
                        String toolName = parts[1];
                        log.info("需要调用工具: {}", toolName);
                        
                        if ("queryAvailableRoomTypes".equals(toolName)) {
                            // 调用房型查询工具
                            String roomTypesInfo = roomTypeQueryTool.queryAvailableRoomTypes();
                            log.info("查询到房型信息: {}", roomTypesInfo);
                            
                            // 发送房型信息给用户
                            HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, roomTypesInfo, "router");
                            
                            // 标记需要用户确认/选择
                            context.setNeedConfirmation(true);
                            context.setConfirmationType("INPUT_PARAMS");
                            context.setPausedAtNode("check_params");
                            context.setCurrentStep("请选择您想要预订的房型");
                        } else {
                            log.warn("未知的工具名称: {}", toolName);
                            handleMissingParams(context, currentTask, "MISSING||请提供必要的信息哦～");
                        }
                    }
                } else if (checkResult.startsWith("EXTRACT")) {
                    // AI 提取到了参数，合并到任务参数中
                    String[] parts = checkResult.split("\\|", 2);
                    if (parts.length > 1) {
                        try {
                            String extractedJson = parts[1];
                            Map<String, Object> extractedParams = JSONUtil.toBean(extractedJson, Map.class);
                            
                            // 合并提取到的参数
                            currentTask.getParams().putAll(extractedParams);
                            log.info("从用户输入中提取到参数: {}, 合并后的完整参数: {}", extractedParams, currentTask.getParams());
                            
                            // 再次检查参数是否完整
                            String newParamsJson = JSONUtil.toJsonStr(currentTask.getParams());
                            String recheckResult = agent.checkTaskParams(
                                    currentTask.getTaskType(),
                                    currentTask.getDescription(),
                                    newParamsJson,
                                    userMessage != null ? userMessage : "",
                                    conversationHistory != null ? conversationHistory : "",
                                    "请再次检查参数是否完整"
                            );
                            
                            if (recheckResult.startsWith("COMPLETE")) {
                                // 参数已完整
                                log.info("参数提取完成，准备执行任务");
                                context.setNeedConfirmation(false);
                                currentTask.setStatus("RUNNING");
                                context.setCurrentStep("开始执行任务: " + currentTask.getDescription());
                            } else if (recheckResult.startsWith("MISSING")) {
                                // 仍然有参数缺失
                                handleMissingParams(context, currentTask, recheckResult);
                            }
                        } catch (Exception e) {
                            log.error("解析提取的参数失败", e);
                            context.setCurrentStep("参数解析失败: " + e.getMessage());
                        }
                    }
                } else if (checkResult.startsWith("MISSING")) {
                    // 参数缺失，需要等待用户输入
                    handleMissingParams(context, currentTask, checkResult);
                } else {
                    // 参数完整，可以继续执行
                    log.info("参数检查通过，准备执行任务");
                    context.setNeedConfirmation(false);
                    currentTask.setStatus("RUNNING");
                    context.setCurrentStep("开始执行任务: " + currentTask.getDescription());
                }

            } catch (Exception e) {
                log.error("参数检查失败", e);
                context.setCurrentStep("参数检查失败: " + e.getMessage());
            }

            return HotelAssistantContext.saveContext(context);
        });
    }

    /**
     * 处理参数缺失的情况
     */
    private void handleMissingParams(HotelAssistantContext context, AgentTask currentTask, String checkResult) {
        String[] parts = checkResult.split("\\|");
        String missingParams = parts.length > 1 ? parts[1] : "";
        String promptMessage = parts.length > 2 ? parts[2] : "请提供必要的信息哦～";

        log.info("参数缺失: {}, 提示用户: {}", missingParams, promptMessage);

        // 发送 SSE 事件提示用户输入
        HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, promptMessage, "router");

        // 标记需要用户确认
        context.setNeedConfirmation(true);
        context.setConfirmationType("INPUT_PARAMS");
        context.setPausedAtNode("check_params");
        context.setCurrentStep(promptMessage);
    }
}
