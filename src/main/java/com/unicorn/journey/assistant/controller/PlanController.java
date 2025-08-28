package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;


    /**
     * 创建plan
     */
    @PostMapping("/plan/create")
    public Result createPlan(@RequestBody Plan plan) {
        planService.createPlan(plan);
        return Result.ok();
    }

    /**
     * 根据用户ID获取用户的所有plan信息
     */
    @GetMapping("/plan/get")
    public Result getPlansByUser(@RequestParam int userId) {
        List<Plan> plans = planService.retrievePlansByUserId(userId);
        return Result.ok(plans);
    }

    /**
     * 根据planId获取指定plan信息
     */
    @GetMapping("/plan/get/{planId}")
    public Result getPlanById(@PathVariable int planId) {
        Plan plan = planService.retrievePlanById(planId);
        return Result.ok(plan);
    }

}
