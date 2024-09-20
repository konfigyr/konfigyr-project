package com.konfigyr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.modulith.Modulith;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/**
 * Konfigyr Spring server application main class.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EnableCaching
@EnableJdbcHttpSession
@SpringBootApplication
@Modulith(systemName = "Konfigyr")
public class KonfigyrApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonfigyrApplication.class, args);
	}

}
