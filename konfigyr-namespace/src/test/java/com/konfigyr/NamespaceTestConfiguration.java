package com.konfigyr;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Vladimir Spasic
 **/
@TestProfile
@ComponentScan
@EnableAutoConfiguration
@ImportTestcontainers(TestContainers.class)
@Configuration(proxyBeanMethods = false)
public class NamespaceTestConfiguration {

	@Bean
	SecurityFilterChain security(@NonNull HttpSecurity http) throws Exception {
		return http.build();
	}

}
