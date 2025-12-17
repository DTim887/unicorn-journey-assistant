package com.unicorn.journey.assistant.hotel.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.enums.VoiceCharacter;
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
     * @param request 聊天请求（包含userId, message, sessionId, enableVoiceOutput, voiceCharacter）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: userId={}, message={}, sessionId={}, enableVoiceOutput={}, voiceCharacter={}",
                request.getUserId(), request.getMessage(), request.getSessionId(),
                request.getEnableVoiceOutput(), request.getVoiceCharacter());

        // 解析语音角色
        VoiceCharacter voiceCharacter = parseVoiceCharacter(request.getVoiceCharacter());
        SseEmitter sseEmitter =
                hotelAssistantService.chat(
                        request.getUserId(),
                        request.getMessage(),
                        request.getSessionId(),
                        request.getEnableVoiceOutput() != null ? request.getEnableVoiceOutput() : false,
                        voiceCharacter
                );
        return sseEmitter;
    }

    /**
     * 语音聊天接口 - SSE返回
     * 支持语音输入，通过输出参数控制是否启用语音输出
     *
     * @param file               语音文件
     * @param userId             用户ID
     * @param sessionId          会话ID（可选）
     * @param enableVoiceOutput  是否启用语音输出，默认true
     * @param voiceCharacterName 语音角色，默认NICK
     */
    @PostMapping(value = "/voice-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter voiceChat(
            @RequestParam("audio") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "enableVoiceOutput", defaultValue = "true") boolean enableVoiceOutput,
            @RequestParam(value = "voiceCharacter", required = false) String voiceCharacterName) throws IOException {

        log.info("收到语音聊天请求: userId={}, sessionId={}, enableVoiceOutput={}, voiceCharacter={}",
                userId, sessionId, enableVoiceOutput, voiceCharacterName);

        // 语音转文字
        String userMessage = sttService.speechToText(file);
        log.info("语音识别结果: {}", userMessage);

        // 解析语音角色
        VoiceCharacter voiceCharacter = parseVoiceCharacter(voiceCharacterName);

        return hotelAssistantService.chat(
                userId,
                userMessage,
                sessionId,
                enableVoiceOutput,
                voiceCharacter
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

    /**
     * 语音转文字接口
     * 独立的API，可供前端单独调用
     *
     * @param file 语音文件
     * @return 识别的文本内容
     */
    @PostMapping("/speech-to-text")
    public Result speechToText(@RequestParam("audio") MultipartFile file) {
        try {
            log.info("收到语音转文字请求: fileName={}, size={}",
                    file.getOriginalFilename(), file.getSize());

            String text = sttService.speechToText(file);
            log.info("语音识别成功: {}", text);

            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());

            return Result.ok(data);
        } catch (IOException e) {
            log.error("语音转文字失败: {}", e.getMessage(), e);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());
            return Result.builder()
                    .code("500")
                    .msg("语音识别失败")
                    .data(errorData)
                    .build();
        }
    }

    @PostMapping("/switch_status")
    public Result switchStatus(@RequestParam("userId") String userId, @RequestParam("status") String status) {
        hotelAssistantService.switchStatus(userId, status);
        return Result.ok();
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
