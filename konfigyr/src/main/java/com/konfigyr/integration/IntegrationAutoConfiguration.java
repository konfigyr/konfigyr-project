package com.konfigyr.integration;

import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class IntegrationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(IntegrationManager.class)
	IntegrationManager defaultIntegrationManager(DSLContext context) {
		return new DefaultIntegrationManager(context);
	}

}
