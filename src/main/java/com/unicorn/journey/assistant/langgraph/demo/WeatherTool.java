package com.unicorn.journey.assistant.langgraph.demo;

import dev.langchain4j.agent.tool.Tool;

import java.util.Random;

public class WeatherTool {

    private static final Random RANDOM = new Random();

    @Tool("获取指定城市当前的天气信息")
    public String getCurrentWeather(String city) {
        System.out.printf("[Tool Call] Getting weather for %s%n", city);
        if ("上海".equalsIgnoreCase(city)) {
            int randomIndex = RANDOM.nextInt(10);
            if(randomIndex < 5) {
                return "上海今天多云转晴，气温 28 摄氏度。";
            }
            return "上海今天阴天，局部地区有大雨。";

        }
        return String.format("%s 的天气信息未知。", city);
    }
}
