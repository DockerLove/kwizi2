package com.example.kwizi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class KwiziApplication {

	public static void main(String[] args) {
		SpringApplication.run(KwiziApplication.class, args);
	}

}
