package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.PlanVO;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Attraction;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.entity.mappers.PlanMapper;
import com.unicorn.journey.assistant.service.AttractionService;
import com.unicorn.journey.assistant.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    private final AttractionService attractionService;

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
        Plan plan = planService.retrievePlanByUserId(userId);
        PlanVO planVO = PlanMapper.INSTANCE.convertToPlanVO(plan);
        List<PlanVO.PlanAttractionItemVO> planAttractionItemVOS = new ArrayList<>();
        plan.getAttractionIds().forEach(attractionId -> {
            PlanVO.PlanAttractionItemVO planAttractionItemVO = new PlanVO.PlanAttractionItemVO();
            Attraction attraction = attractionService.retrieveAttractionById(attractionId.getAttractionId());
            planAttractionItemVO.setAttractionId(attraction.getAttractionId());
            planAttractionItemVO.setAttractionName(attraction.getAttractionName());
            planAttractionItemVO.setImage(attraction.getImage());
            planAttractionItemVO.setQueueTime(attraction.getQueueTime());
            planAttractionItemVO.setVisitTimeRange(attractionId.getVisitTimeRange());
            planAttractionItemVO.setTags(attraction.getTags());
            planAttractionItemVOS.add(planAttractionItemVO);
        });
        planVO.setPlanAttractionItemVO(planAttractionItemVOS);
        return Result.ok(planVO);
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
