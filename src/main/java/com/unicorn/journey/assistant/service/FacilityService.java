package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Facility;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.FACILITY)
public class FacilityService extends BaseService<Facility> {


    /**
     * save facility info
     * @param facility facility mete data
     */
    public void saveFacility(Facility facility) {
        this.put(facility.getFacilityName(), facility);
    }

    @Tool("Get facility by facilityName")
    public Facility retrieveFacilityByName(String facilityName) {
        return this.get(facilityName);
    }

    @Tool("Get all facility")
    public List<Facility> retrieveAllFacility() {
        return this.getAll(Facility.class);
    }
}
