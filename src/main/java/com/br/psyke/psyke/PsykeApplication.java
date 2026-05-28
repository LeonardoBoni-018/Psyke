package com.br.psyke.psyke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PsykeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PsykeApplication.class, args);
	}

}
