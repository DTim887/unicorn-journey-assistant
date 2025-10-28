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
        Thread.startVirtualThread(() -> workflowHandleService.executeWorkflow(sessionId, request.getUserId(), emitter));

        return emitter;
    }

    /**
     * 用户确认接口
     */
    @PostMapping("/confirm/{sessionId}")
    public Map<String, Object> confirmAction(
            @PathVariable String sessionId,
            @RequestBody ConfirmRequest confirmRequest) {
        log.info("收到用户确认: sessionId={}, confirmed={}, type={}", 
            sessionId, confirmRequest.isConfirmed(), confirmRequest.getConfirmType());

        // 获取暂停的工作流状态
        MessagesState<String> pausedState = checkpointService.removePausedState(sessionId);
        SseEmitter emitter = checkpointService.getEmitter(sessionId);

        if (pausedState == null || emitter == null) {
            log.warn("未找到工作流状态或连接: sessionId={}", sessionId);
            return Map.of("success", false, "message", "会话不存在或已过期");
        }

        // 更新确认结果
        ConfirmWorkflowContext context = ConfirmWorkflowContext.getContext(pausedState);
        context.setConfirmationResult(confirmRequest.isConfirmed() ?
                ConfirmTypeEnum.APPROVED.getCode() : ConfirmTypeEnum.REJECTED.getCode());

        // 在虚拟线程中恢复工作流执行
        Thread.startVirtualThread(() -> workflowHandleService.resumeWorkflow(sessionId, pausedState, emitter));

        return Map.of(
            "success", true, 
            "message", confirmRequest.isConfirmed() ? "确认成功" : "已拒绝",
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
