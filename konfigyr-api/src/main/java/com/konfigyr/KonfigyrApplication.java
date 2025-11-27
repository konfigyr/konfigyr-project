package com.konfigyr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Konfigyr Spring REST API server application main class.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EnableAsync
@EnableCaching
@SpringBootApplication
@EnableTransactionManagement
@EnableSpringDataWebSupport
@Modulith(systemName = "Konfigyr REST API")
public class KonfigyrApplication {

	static void main(String[] args) {
		SpringApplication.run(KonfigyrApplication.class, args);
	}

}
