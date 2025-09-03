package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.AssistantExchangeRequest;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Assistant;
import com.unicorn.journey.assistant.service.AssistantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssistantController {


    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    //switch assistant
    @PostMapping("/assistant/exchange")
    public Result exchange(@RequestBody AssistantExchangeRequest assistantExchangeRequest) {
        Assistant assistant = assistantService.retrieveAssistantByName(assistantExchangeRequest.getAssistantName());
        if (assistant != null) {
            assistantService.exchange(assistant);
        }
        return Result.ok();
    }

    //Get current assistant
    @GetMapping("/assistant/current")
    public Result currentAssistant() {
        Assistant currentAssistant = assistantService.currentAssistant();
        return Result.ok(currentAssistant);
    }

    //Get all assistant in system
    @GetMapping("/assistant/all")
    public Result allAssistant() {
        List<Assistant> assistants = assistantService.retrieveAllAssistants();
        return Result.ok(assistants);
    }
}
