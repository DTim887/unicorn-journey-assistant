package com.unicorn.journey.assistant.langgragh4j.controller;

import com.unicorn.journey.assistant.langgragh4j.enums.ConfirmTypeEnum;
import com.unicorn.journey.assistant.langgragh4j.model.request.StartWorkflowRequest;
import com.unicorn.journey.assistant.langgragh4j.model.response.ConfirmRequest;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowCheckpointService;
import com.unicorn.journey.assistant.langgragh4j.service.WorkflowHandleService;
import com.unicorn.journey.assistant.langgragh4j.state.ConfirmWorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 流式确认工作流控制器（人在环路实现）
 * 使用工作流的自然暂停和恢复机制，而不是invoke阻塞线程
 */
@RestController
@RequestMapping("/workflow/stream-confirm")
@Slf4j
@RequiredArgsConstructor
public class StreamConfirmWorkflowController {

    private final WorkflowCheckpointService checkpointService;
    private final WorkflowHandleService workflowHandleService;

    /**
     * 启动流式工作流
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startWorkflow(@RequestBody StartWorkflowRequest request) {
        String sessionId = generateSessionId(request.getUserId());
        log.info("请求启动工作流:{}", sessionId);

        // 生成60分钟超时 SSE
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
        checkpointService.saveEmitter(sessionId, emitter);
        log.info("SseEmitter 创建成功，sessionId={}, 超时时间: 60分钟", sessionId);

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
        log.info("异步执行工作流...");
        Thread.startVirtualThread(() -> workflowHandleService.executeWorkflow(
            sessionId, 
            request.getUserId(), 
            request.getVisitDate(), 
            request.getVisitorCount(), 
            emitter
        ));

        return emitter;
    }

    /**
     * 用户确认接口（支持确认/拒绝/重新生成）
     */
    @PostMapping("/confirm/{sessionId}")
    public Map<String, Object> confirmAction(
            @PathVariable String sessionId,
            @RequestBody ConfirmRequest confirmRequest) {
        log.info("收到用户操作: sessionId={}, action={}, confirmed={}, type={}, visitDate={}, visitorCount={}", 
            sessionId, confirmRequest.getAction(), confirmRequest.isConfirmed(), 
            confirmRequest.getConfirmType(), confirmRequest.getVisitDate(), confirmRequest.getVisitorCount());

        // 获取暂停的工作流状态
        MessagesState<String> pausedState = checkpointService.removePausedState(sessionId);
        SseEmitter emitter = checkpointService.getEmitter(sessionId);

        if (pausedState == null || emitter == null) {
            log.warn("未找到工作流状态或连接: sessionId={}", sessionId);
            return Map.of("success", false, "message", "会话不存在或已过期");
        }

        // 更新上下文
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(pausedState);
        
        // 优先使用 action 字段，如果没有则使用 confirmed 字段兼容旧版本
        String action = confirmRequest.getAction();
        if (action == null) {
            action = confirmRequest.isConfirmed() ? 
                ConfirmTypeEnum.APPROVED.getCode() : ConfirmTypeEnum.REJECTED.getCode();
        }
        
        // 处理不同的操作类型
        String message = "处理中...";
        
        // 检查是否是输入日期或人数的情况
        boolean isInputAction = confirmRequest.getVisitDate() != null || confirmRequest.getVisitorCount() != null;
        
        if (isInputAction) {
            // 处理输入日期或人数
            if (confirmRequest.getVisitDate() != null) {
                context.setVisitDate(confirmRequest.getVisitDate());
                log.info("更新游玩日期: {}", confirmRequest.getVisitDate());
                message = "游玩日期已设置";
            }
            if (confirmRequest.getVisitorCount() != null) {
                context.setVisitorCount(confirmRequest.getVisitorCount());
                log.info("更新游玩人数: {}", confirmRequest.getVisitorCount());
                message = "游玩人数已设置";
            }
            // 清除确认标志，让工作流继续
            context.setNeedConfirmation(false);
            context.setConfirmationType(null);
        } else {
            // 处理确认/拒绝/重新生成
            switch (action.toLowerCase()) {
                case "approved":
                    context.setConfirmationResult(ConfirmTypeEnum.APPROVED.getCode());
                    message = "确认成功";
                    break;
                case "rejected":
                    context.setConfirmationResult(ConfirmTypeEnum.REJECTED.getCode());
                    message = "已拒绝";
                    break;
                case "regenerate":
                    context.setConfirmationResult(ConfirmTypeEnum.REGENERATE.getCode());
                    message = "正在重新生成...";
                    break;
                default:
                    log.warn("未知的操作类型: {}", action);
                    message = "操作已接收";
                    break;
            }
        }

        // 在虚拟线程中恢复工作流执行
        Thread.startVirtualThread(() -> workflowHandleService.resumeWorkflow(sessionId, pausedState, emitter));

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
        return userId + "_" + System.currentTimeMillis();
    }

}
