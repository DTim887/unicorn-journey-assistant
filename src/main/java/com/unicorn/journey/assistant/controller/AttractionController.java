package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Attraction;
import com.unicorn.journey.assistant.service.AttractionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttractionController {

    @Resource
    AttractionService attractionService;

    @GetMapping("/attraction/all")
    public Result allAttraction() {
        List<Attraction> allAttractions = attractionService.getAll(Attraction.class);
        return Result.ok(allAttractions);
    }

}
