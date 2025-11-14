package com.unicorn.journey.assistant.langgragh4j.hotel.controller;

import com.unicorn.journey.assistant.langgragh4j.hotel.model.HotelAssistantRequest;
import com.unicorn.journey.assistant.langgragh4j.hotel.model.HotelConfirmRequest;
import com.unicorn.journey.assistant.langgragh4j.hotel.service.HotelWorkflowService;
import com.unicorn.journey.assistant.langgragh4j.hotel.state.HotelAssistantContext;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 酒店智能助手控制器
 */
@RestController
@RequestMapping("/hotel-assistant")
@Slf4j
@RequiredArgsConstructor
public class HotelAssistantController {

    private final WorkflowCheckpointService checkpointService;
    private final HotelWorkflowService hotelWorkflowService;

    /**
     * 启动酒店助手
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter start(@RequestBody HotelAssistantRequest request) {
        String sessionId = generateSessionId(request.getUserId());
        log.info("启动酒店助手: sessionId={}, userMessage={}", sessionId, request.getUserMessage());

        // 创建 SSE 连接（60分钟超时）
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        checkpointService.saveEmitter(sessionId, emitter);

        // 设置回调
        emitter.onCompletion(() -> {
            log.info("SSE连接正常完成: sessionId={}", sessionId);
            checkpointService.removeEmitter(sessionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: sessionId={}", sessionId);
            checkpointService.cleanupSession(sessionId);
            try {
                emitter.completeWithError(new Exception("连接超时"));
            } catch (Exception e) {
                log.error("处理超时异常时出错", e);
            }
        });

        emitter.onError((e) -> {
            log.error("SSE连接错误: sessionId={}, error={}", sessionId, e.getMessage(), e);
            checkpointService.cleanupSession(sessionId);
        });

        // 在虚拟线程中执行工作流
        Thread.startVirtualThread(() -> hotelWorkflowService.executeWorkflow(
                sessionId,
                request.getUserId(),
                request.getUserMessage(),
                emitter
        ));

        return emitter;
    }

    /**
     * 用户确认/输入接口
     */
    @PostMapping("/confirm/{sessionId}")
    public Map<String, Object> confirm(
            @PathVariable String sessionId,
            @RequestBody HotelConfirmRequest confirmRequest) {
        log.info("收到用户操作: sessionId={}, action={}, confirmType={}, params={}",
                sessionId, confirmRequest.getAction(), confirmRequest.getConfirmType(), confirmRequest.getParams());

        // 获取暂停的工作流状态
        MessagesState<String> pausedState = checkpointService.getPausedState(sessionId);
        SseEmitter emitter = checkpointService.getEmitter(sessionId);

        if (pausedState == null || emitter == null) {
            log.warn("未找到工作流状态或连接: sessionId={}", sessionId);
            return Map.of("success", false, "message", "会话不存在或已过期");
        }

        // 更新上下文
        HotelAssistantContext context = HotelAssistantContext.getContext(pausedState);
        
        // 记录用户输入到对话历史
        if (confirmRequest.getUserInput() != null && !confirmRequest.getUserInput().trim().isEmpty()) {
            context.addUserInput(confirmRequest.getUserInput());
            context.setUserMessage(confirmRequest.getUserInput());
            log.info("记录用户输入: {}", confirmRequest.getUserInput());
        }

        String action = confirmRequest.getAction();
        String message = "处理中...";

        // 处理参数输入
        if (confirmRequest.getParams() != null && !confirmRequest.getParams().isEmpty()) {
            // 更新当前任务的参数
            HotelAssistantContext.AgentTask currentTask = context.getCurrentTask();
            if (currentTask != null) {
                currentTask.getParams().putAll(confirmRequest.getParams());
                log.info("更新任务参数: {}", confirmRequest.getParams());
                message = "参数已更新";
            }
            context.setNeedConfirmation(false);
        } else {
            // 处理确认/拒绝/重新生成
            switch (action.toLowerCase()) {
                case "approved":
                    context.setConfirmationResult("approved");
                    message = "确认成功";
                    break;
                case "rejected":
                    context.setConfirmationResult("rejected");
                    message = "已拒绝";
                    break;
                case "regenerate":
                    context.setConfirmationResult("regenerate");
                    message = "正在重新生成...";
                    break;
                default:
                    log.warn("未知的操作类型: {}", action);
                    message = "操作已接收";
                    break;
            }
        }

        // 在虚拟线程中恢复工作流执行
        Thread.startVirtualThread(() -> hotelWorkflowService.resumeWorkflow(sessionId, pausedState, emitter));

        return Map.of(
                "success", true,
                "message", message,
                "action", action,
                "sessionId", sessionId
        );
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId(String userId) {
        return "HOTEL_" + userId + "_" + System.currentTimeMillis();
    }
}
