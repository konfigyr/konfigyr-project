package com.konfigyr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Konfigyr Spring server application main class.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EnableCaching
@SpringBootApplication
public class KonfigyrApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonfigyrApplication.class, args);
	}

}
