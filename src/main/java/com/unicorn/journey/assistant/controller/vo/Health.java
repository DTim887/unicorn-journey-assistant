package com.unicorn.journey.assistant.controller.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Health {

    private int status;

    public Health(int status) {
        this.status = status;
    }

    public static class HealthInstance {
        private final static Health health = new Health(0);
    }

    public static Health ok() {
        return HealthInstance.health;
    }
}
