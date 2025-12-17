package com.unicorn.journey.assistant.hotel.entity;

import lombok.Data;

import java.util.List;

@Data
public class FacilityWaitTime {

    List<Facility> entries;

    @Data
    public static class Facility {

        private String id;
        private WaitTime waitTime;

        @Data
        public static class WaitTime {

            private String status;
            private int postedWaitMinutes;


        }
    }
}
