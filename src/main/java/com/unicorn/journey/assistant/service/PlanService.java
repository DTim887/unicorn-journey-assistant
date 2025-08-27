package com.unicorn.journey.assistant.service;

import cn.hutool.core.util.ObjectUtil;
import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Plan;
import com.unicorn.journey.assistant.exception.ErrorCode;
import com.unicorn.journey.assistant.exception.ThrowUtils;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@LocalCache(value = CacheName.PLAN)
@Service
public class PlanService extends BaseService<Plan>{

    @Tool("为单个指定用户，创建指定某一天的行程，planName为空会使用点亮心中绮梦作为默认名称，景点id不能为空，且景点顺序是attractionIds中attractionId的顺序")
    String createPlan(int userId, String planName, List<Integer> attractionIds, LocalDate planDate) {
        String planId = UUID.randomUUID().toString();

        ThrowUtils.throwIf(ObjectUtil.isEmpty(attractionIds), ErrorCode.ATTRACTION_IS_NULL_ERROR);

        Plan plan = Plan.builder()
                .planId(planId)
                .planName(planName)
                .planDate(planDate)
                .attractionIds(attractionIds)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        put(plan.getPlanId(), plan);

        return planId;
    }
}
