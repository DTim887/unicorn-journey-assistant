package com.unicorn.journey.assistant.hotel.entity;

import lombok.Data;

@Data
public class FacilityQueueTime {

    private String facilityId;
    private String facilityName;
    private String status;
    private int waitTimeMinutes;
}
