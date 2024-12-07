package com.konfigyr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Konfigyr Spring server application main class.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EnableAsync
@EnableCaching
@EnableJdbcHttpSession
@SpringBootApplication
@EnableTransactionManagement
@Modulith(systemName = "Konfigyr")
public class KonfigyrApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonfigyrApplication.class, args);
	}

}
