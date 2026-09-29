package com.socialapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {


	// --- ACESTA ESTE BLOCUL NOU ---
	// Se va executa înaintea metodei main, garantând aplicarea setărilor
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");
	}
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
