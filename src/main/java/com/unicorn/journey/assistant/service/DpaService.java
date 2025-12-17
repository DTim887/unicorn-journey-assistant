package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.DPA;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@LocalCache(value = CacheName.DPA)
public class DpaService extends BaseService<DPA> {


    @Tool("根据景点ID获取在售的景点尊享卡产品信息")
    public DPA getDpaByFacilityId(String facilityId) {
        return this.get(facilityId);
    }

    @Tool("根据facilityId购买尊享卡产品，下单成功返回订单号")
    public String buyDpa(String facilityId) {
        return UUID.randomUUID().toString();
    }

    //保存dpa到缓存
    public void saveDpa(DPA dpa) {
        this.put(dpa.getFacilityId(),dpa);
    }
}
