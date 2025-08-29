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

//    @Tool("Get attraction by attractionId")
    public Attraction retrieveAttractionById(String attractionId) {
        return this.get(attractionId);
    }

    @Tool("Get all attractionId")
    public List<Attraction> retrieveAllAttraction() {
        return this.getAll(Attraction.class);
    }
}
