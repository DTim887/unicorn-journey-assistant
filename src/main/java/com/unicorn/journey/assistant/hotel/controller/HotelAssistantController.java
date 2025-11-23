package com.unicorn.journey.assistant.hotel.controller;

import com.unicorn.journey.assistant.hotel.dto.ChatRequest;
import com.unicorn.journey.assistant.hotel.entity.WakeUpAssistance;
import com.unicorn.journey.assistant.hotel.service.HotelAssistantService;
import com.unicorn.journey.assistant.service.STTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 酒店助手controller
 */
@Slf4j
@RestController
@RequestMapping("/hotel")
public class HotelAssistantController {
    
    private final HotelAssistantService hotelAssistantService;
    private final STTService sttService;
    
    public HotelAssistantController(HotelAssistantService hotelAssistantService, STTService sttService) {
        this.hotelAssistantService = hotelAssistantService;
        this.sttService = sttService;
    }
    
    /**
     * 聊天接口 - SSE返回
     * sessionId为空时创建新会话，否则使用已有会话
     * 对话内容同步返回，结构化数据异步发送
     * 
     * @param request 聊天请求（包含userId, message, sessionId, enableVoiceOutput）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: userId={}, message={}, sessionId={}, enableVoiceOutput={}", 
                request.getUserId(), request.getMessage(), request.getSessionId(), request.getEnableVoiceOutput());
        
        return hotelAssistantService.chat(
                request.getUserId(), 
                request.getMessage(), 
                request.getSessionId(),
                request.getEnableVoiceOutput() != null ? request.getEnableVoiceOutput() : false
        );
    }
    
    /**
     * 语音聊天接口 - SSE返回
     * 支持语音输入，通过输出参数控制是否启用语音输出
     * 
     * @param file 语音文件
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param enableVoiceOutput 是否启用语音输出，默认true
     */
    @PostMapping(value = "/voice-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter voiceChat(
            @RequestParam("audio") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "enableVoiceOutput", defaultValue = "true") boolean enableVoiceOutput) throws IOException {
        
        log.info("收到语音聊天请求: userId={}, sessionId={}, enableVoiceOutput={}", userId, sessionId, enableVoiceOutput);
        
        // 语音转文字
        String userMessage = sttService.speechToText(file);
        log.info("语音识别结果: {}", userMessage);
        
        return hotelAssistantService.chat(
                userId, 
                userMessage, 
                sessionId,
                enableVoiceOutput
        );
    }


    /**
     * 获取用户叫醒服务列表
     */
    @GetMapping("/wakeup-assistance/{userId}")
    public List<WakeUpAssistance> getUserWakeUpWakeUpAssistanceMap(@PathVariable String userId) {
        return hotelAssistantService.getUserWakeUpAssistanceMap(userId);
    }
    
    /**
     * 获取叫醒服务详情
     */
    @GetMapping("/wakeup-service/{wakeUpId}")
    public WakeUpAssistance getWakeUpAssistance(@PathVariable String wakeUpId) {
        return hotelAssistantService.getWakeUpAssistance(wakeUpId);
    }
    
    /**
     * 删除叫醒服务
     * 前端执行完叫醒服务后调用
     */
    @DeleteMapping("/wakeup-service/{wakeUpId}")
    public Map<String, Object> deleteWakeUpService(@PathVariable String wakeUpId) {
        log.info("删除叫醒服务: wakeUpId={}", wakeUpId);
        
        boolean success = hotelAssistantService.deleteWakeUpService(wakeUpId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("wakeUpId", wakeUpId);
        result.put("message", success ? "叫醒服务已删除" : "叫醒服务不存在");
        
        return result;
    }
    
    /**
     * 清除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        hotelAssistantService.clearSession(sessionId);
    }
}
