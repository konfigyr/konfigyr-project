package com.konfigyr.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication
@Modulith(systemName = "Konfigyr Identity")
public class KonfigyrIdentityApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonfigyrIdentityApplication.class, args);
	}

}
