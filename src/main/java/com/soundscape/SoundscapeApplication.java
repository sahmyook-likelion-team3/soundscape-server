package com.soundscape;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SoundscapeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundscapeApplication.class, args);
	}

}
