package com.unicorn.journey.assistant.hotel.controller;

import com.unicorn.journey.assistant.enums.VoiceCharacter;
import com.unicorn.journey.assistant.hotel.dto.ChatRequest;
import com.unicorn.journey.assistant.hotel.service.UserStatusRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 用户状态路由Controller
 * 根据用户状态(IN_PARK/IN_ROOM)路由到不同的服务
 */
@Slf4j
@RestController
@RequestMapping("/hotel")
public class UserStatusRouterController {

    private final UserStatusRouterService userStatusRouterService;

    public UserStatusRouterController(UserStatusRouterService userStatusRouterService) {
        this.userStatusRouterService = userStatusRouterService;
    }

    /**
     * 聊天接口 - SSE返回
     * 根据用户状态(IN_PARK/IN_ROOM)路由到不同的业务逻辑
     * sessionId为空时创建新会话，否则使用已有会话
     * 对话内容同步返回，结构化数据异步发送
     * 默认启用语音输出
     *
     * @param request 聊天请求（包含userId, message, sessionId, voiceCharacter）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: userId={}, message={}, sessionId={}, voiceCharacter={}",
                request.getUserId(), request.getMessage(), request.getSessionId(),
                request.getVoiceCharacter());

        // 解析语音角色
        VoiceCharacter voiceCharacter = parseVoiceCharacter(request.getVoiceCharacter());
        
        // 默认启用语音输出
        SseEmitter sseEmitter = userStatusRouterService.chat(
                request.getUserId(),
                request.getMessage(),
                request.getSessionId(),
                true,  // 默认启用语音输出
                voiceCharacter
        );
        
        return sseEmitter;
    }

    /**
     * 解析语音角色字符串
     */
    private VoiceCharacter parseVoiceCharacter(String voiceCharacterName) {
        if (voiceCharacterName == null || voiceCharacterName.trim().isEmpty()) {
            return VoiceCharacter.NICK; // 默认使用尼克
        }

        try {
            return VoiceCharacter.valueOf(voiceCharacterName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的语音角色: {}, 使用默认值 NICK", voiceCharacterName);
            return VoiceCharacter.NICK;
        }
    }
}
