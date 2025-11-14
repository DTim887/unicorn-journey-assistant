package com.unicorn.journey.assistant.langgragh4j.hotel.node;

import cn.hutool.json.JSONUtil;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.HotelAgentFactory;
import com.unicorn.journey.assistant.langgragh4j.hotel.agent.SummaryAgent;
import com.unicorn.journey.assistant.langgragh4j.hotel.enums.HotelSSEEventType;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.hotel.util.HotelSseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 汇总节点
 */
@Slf4j
@RequiredArgsConstructor
public class SummaryNode {

    private final HotelAgentFactory agentFactory;
    private final WorkflowCheckpointService checkpointService;

    /**
     * 创建汇总节点
     */
    public AsyncNodeAction<org.bsc.langgraph4j.prebuilt.MessagesState<String>> create() {
        return node_async(state -> {
            HotelAssistantContext context = HotelAssistantContext.getContext(state);
            log.info("执行节点: 汇总节点");

            try {
                // 构建已完成任务的汇总信息
                StringBuilder completedTasksInfo = new StringBuilder();
                for (var task : context.getTasks()) {
                    if ("COMPLETED".equals(task.getStatus()) && task.getResult() != null) {
                        completedTasksInfo.append("【").append(task.getDescription()).append("】\n");
                        completedTasksInfo.append(task.getResult()).append("\n\n");
                    }
                }

                // 调用 AI 生成汇总
                SummaryAgent agent = agentFactory.getSummaryAgent(context.getSessionId());
                String summary = agent.generateSummary(
                        context.getUserMessage(),
                        completedTasksInfo.toString(),
                        "请生成服务汇总"
                );

                log.info("汇总生成完成，内容长度: {}", summary.length());

                // 发送 SSE 事件
                HotelSseHelper.sendOutputChunk(context.getSessionId(), checkpointService, summary, "summary");

                context.setSummary(summary);
                context.setCurrentStep("服务汇总完成");

            } catch (Exception e) {
                log.error("生成汇总失败", e);
                context.setCurrentStep("生成汇总失败: " + e.getMessage());
            }

            return HotelAssistantContext.saveContext(context);
        });
    }
}
