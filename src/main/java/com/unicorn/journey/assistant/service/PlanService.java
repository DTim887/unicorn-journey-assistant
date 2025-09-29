package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Plan;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

@LocalCache(value = CacheName.PLAN)
@Service
public class PlanService extends BaseService<Plan>{

    @Tool("为单个指定用户，创建指定某一天的行程。id用雪花算法生成，planName不能为空，景点id不能为空，且景点顺序是attractionIds中attractionId的顺序")
    public void createPlan(Plan plan) {
        put(plan.getUserId(), plan);
    }

    /**
     * 查询单个用户的行程, 每个用户只能有一个行程
     * @param userId
     * @return
     */
    public Plan retrievePlanByUserId(int userId) {
        return this.get(userId);
    }


    /**
     * 获取指定id的行程
     * @param id plan id
     * @return 指定plan
     */
    public Plan retrievePlanById(int id) {
        return this.get(id);
    }

}
