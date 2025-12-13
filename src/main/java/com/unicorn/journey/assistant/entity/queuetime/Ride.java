package com.unicorn.journey.assistant.entity.queuetime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ride {
    private int id;
    private String name;
    @JsonProperty("is_open")
    private boolean isOpen;
    @JsonProperty("wait_time")
    private int waitTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWaitTime() {
        return waitTime;
    }
    @JsonProperty("wait_time")
    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public boolean isOpen() {
        return isOpen;
    }
    @JsonProperty("is_open")
    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
}
