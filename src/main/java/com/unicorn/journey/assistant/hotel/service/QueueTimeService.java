package com.unicorn.journey.assistant.hotel.service;

import com.unicorn.journey.assistant.hotel.entity.FacilityQueueTime;
import com.unicorn.journey.assistant.hotel.entity.FacilityWaitTime;
import com.unicorn.journey.assistant.hotel.enums.FacilityMapEnum;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueTimeService {

    private final RestClient restClient;

    private static final String QUEUE_TIME_URI = "https://app.apigw.shanghaidisneyresort.com/resource-assembler-platform/public/wait-times/shdr;entityType=destination;destination=shdr?region=CN";

    public QueueTimeService() {
        this.restClient = RestClient.builder()
                .build();
    }

    //等待时间工具
    @Tool("获取景点的排队时间 (e.g. Seven Dwarfs Mine Train)")
    public List<FacilityQueueTime> getAttractionQueueTime() {
        FacilityWaitTime facilityWaitTime = restClient.get()
                .uri(QUEUE_TIME_URI)
                .retrieve()
                .body(FacilityWaitTime.class);
        List<FacilityQueueTime> facilityQueueTimes = new ArrayList<>();
        for (int i = 0; i < facilityWaitTime.getEntries().size(); i++) {
            FacilityWaitTime.Facility facility = facilityWaitTime.getEntries().get(i);
            if (FacilityMapEnum.findNameByFacilityId(facility.getId()) != null) {
                FacilityQueueTime facilityQueueTime = new FacilityQueueTime();
                facilityQueueTime.setFacilityId(facility.getId());
                facilityQueueTime.setFacilityName(FacilityMapEnum.findNameByFacilityId(facility.getId()));
                facilityQueueTime.setWaitTimeMinutes(facility.getWaitTime().getPostedWaitMinutes());
                if("Operating".equals(facility.getWaitTime().getStatus())) {
                    facilityQueueTime.setStatus("运营中");
                }
                if("Closed".equals(facility.getWaitTime().getStatus())) {
                    facilityQueueTime.setStatus("关闭");
                }if("Renewal".equals(facility.getWaitTime().getStatus())) {
                    facilityQueueTime.setStatus("计划性维护中");
                }
                facilityQueueTimes.add(facilityQueueTime);
            }
        }
        return facilityQueueTimes;
    }

}
