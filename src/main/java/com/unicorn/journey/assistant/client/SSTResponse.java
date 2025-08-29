package com.unicorn.journey.assistant.client;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SSTResponse {

    private Result result;

    @Getter
    @Setter
    public static class Result {
        String text;
    }
}
