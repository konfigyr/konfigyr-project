package com.konfigyr.web.thymeleaf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.thymeleaf.dialect.IDialect;

/**
 * Configuration class that would register the {@link KonfigyrDialect} to be used by
 * the {@link org.thymeleaf.Thymeleaf} template engine.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
public class ThymelafConfiguration {

	@Bean
	IDialect konfigyrDialect(Environment environment) {
		return new KonfigyrDialect(environment);
	}

}
