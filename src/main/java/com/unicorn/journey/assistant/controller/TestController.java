package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.ApproveWorkflowDTO;
import com.unicorn.journey.assistant.controller.request.MagicDTO;
import com.unicorn.journey.assistant.entity.queuetime.ParkDataSchema;
import com.unicorn.journey.assistant.langgraph.tour.PlannerApp;
import com.unicorn.journey.assistant.hotel.service.QueueTimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
public class TestController {


    private final QueueTimeService queueTimeService;

    public TestController(QueueTimeService queueTimeService) {
        this.queueTimeService = queueTimeService;
    }


    //和朱迪聊天
    @GetMapping(value = "/magic", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startWorkflow(@RequestParam String visitDate, @RequestParam String peopleNum) {
        SseEmitter sseEmitter = new SseEmitter(300_000L); // 5分钟超时
        MagicDTO magicDTO = new MagicDTO(visitDate, peopleNum);
        new PlannerApp().startWorkflow(sseEmitter,magicDTO);
        return sseEmitter;
    }

    //审批节点
    @PostMapping("/approve")
    public ResponseEntity<String> approveWorkflow(@RequestBody ApproveWorkflowDTO approveWorkflowDTO) {
        return ResponseEntity.ok(new PlannerApp().approveWorkflow(approveWorkflowDTO));
    }

    @GetMapping("/queuetime")
    public ResponseEntity<ParkDataSchema> queueTime() {
        return ResponseEntity.ok(queueTimeService.getAttractionQueueTime());
    }
}
