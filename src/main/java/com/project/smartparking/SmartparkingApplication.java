package com.project.smartparking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartparkingApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartparkingApplication.class, args);
	}

}
