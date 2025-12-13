package com.unicorn.journey.assistant.hotel.service;

import com.unicorn.journey.assistant.entity.queuetime.ParkDataSchema;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class QueueTimeService {

    private final RestClient restClient;


    private static final String QUEUE_TIME_URI = "https://queue-times.com/parks/30/queue_times.json";

    public QueueTimeService() {
        this.restClient = RestClient.builder()
                .build();
    }

    //等待时间工具
    @Tool("获取景点的排队时间 (e.g. Seven Dwarfs Mine Train)")
    public ParkDataSchema getAttractionQueueTime() {
       return restClient.get()
                .uri(QUEUE_TIME_URI)
                .retrieve()
                .body(ParkDataSchema.class);
    }

}
