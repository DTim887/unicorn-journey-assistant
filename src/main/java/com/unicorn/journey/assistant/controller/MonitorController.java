package com.unicorn.journey.assistant.controller;


import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.controller.vo.TokenStatesVO;
import com.unicorn.journey.assistant.service.TokenUsageTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor
@Slf4j
public class MonitorController {
    private final TokenUsageTracker tokenUsageTracker;

    @GetMapping("/token-usage")
    public Result getTokenUsage() {
        return Result.ok(TokenStatesVO.of(
                tokenUsageTracker.getTotalInputTokens(),
                tokenUsageTracker.getTotalOutputTokens(),
                tokenUsageTracker.getTotalTokens()
        ));
    }
}
