package com.unicorn.journey.assistant.langgragh4j.controller;

import com.unicorn.journey.assistant.langgragh4j.ConfirmationWorkflowApp;
import com.unicorn.journey.assistant.langgragh4j.model.request.StartWorkflowRequest;
import com.unicorn.journey.assistant.langgragh4j.model.response.ConfirmRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 工作流 controller
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class ConfirmationWorkflowController {

    private final ConfirmationWorkflowApp workflowApp = new ConfirmationWorkflowApp();

    /**
     * 启动工作流（流式输出）
     * 
     * @param request 包含用户ID和提示词
     * @return SSE 流
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startWorkflow(@RequestBody StartWorkflowRequest request) {
        log.info("收到启动工作流请求: userId={}, prompt={}", request.getUserId(), request.getPrompt());
        return workflowApp.executeWorkflowWithStream(request.getUserId(), request.getPrompt());
    }

    /**
     * 确认请求（流式输出）
     * 
     * @param sessionId 会话ID
     * @param request 确认请求
     * @return SSE 流
     */
    @PostMapping(value = "/confirm/plan/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> confirmPlan(
            @PathVariable String sessionId,
            @RequestBody ConfirmRequest request) {
        log.info("收到确认请求: sessionId={}, confirmed={}, confirmType={}", sessionId, request.isConfirmed(), request.getConfirmType());
        return workflowApp.resumeWorkflow(sessionId, request.isConfirmed(), request.getConfirmType());
    }

}

