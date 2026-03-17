package com.finance.expense_copilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ExpenseCopilotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseCopilotApplication.class, args);
	}

	// ADD THIS BEAN SO THE CONTROLLER CAN USE RESTTEMPLATE
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
