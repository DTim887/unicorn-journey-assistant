package com.unicorn.journey.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UnicornJourneyAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnicornJourneyAssistantApplication.class, args);
	}

}
