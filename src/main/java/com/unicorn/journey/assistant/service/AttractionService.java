package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Attraction;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.ATTRACTION)
public class AttractionService extends BaseService<Attraction> {


    /**
     * save attraction info
     * @param attraction attraction mete data
     */
    public void saveAttraction(Attraction attraction) {
        this.put(attraction.getAttractionId(), attraction);
    }

    @Tool("Get attraction by attractionId")
    public Attraction retrieveAttractionById(Integer attractionId) {
        return this.get(attractionId);
    }

    @Tool("获取所有的景点信息。包括景点ID，景点名称，景点图片，景点描述，排队时间。")
    public List<Attraction> retrieveAllAttraction() {
        return this.getAll(Attraction.class);
    }
}
