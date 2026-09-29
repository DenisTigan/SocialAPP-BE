package com.socialapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {

		// --- FIX PENTRU EROAREA DE CERTIFICAT GOOGLE FCM ---
		System.setProperty("java.net.preferIPv4Stack", "true");

		SpringApplication.run(BackendApplication.class, args);
	}

}
