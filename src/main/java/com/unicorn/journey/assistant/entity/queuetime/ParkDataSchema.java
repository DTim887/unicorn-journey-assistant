package com.unicorn.journey.assistant.entity.queuetime;

import lombok.Data;

import java.util.List;

@Data
public class ParkDataSchema {
    private List<Land> lands;
}

@Data
class Land {
    private int id;
    private String name;
    private List<Ride> rides;
}

