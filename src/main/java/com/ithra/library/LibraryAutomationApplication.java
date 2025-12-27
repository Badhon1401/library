package com.ithra.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LibraryAutomationApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryAutomationApplication.class, args);
	}
}
