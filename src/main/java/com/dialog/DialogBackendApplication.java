package com.dialog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import com.dialog.googleauth.dto.GoogleAuthDTO;

@SpringBootApplication
@EnableConfigurationProperties(GoogleAuthDTO.class)
@ComponentScan(basePackages = {"com.dialog", "com.dialog.exception"})
public class DialogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DialogBackendApplication.class, args);
	}

}
