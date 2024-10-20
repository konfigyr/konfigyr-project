package com.konfigyr.registry;

import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RegistryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(Artifactory.class)
	Artifactory defaultArtifactory(DSLContext context) {
		return new DefaultArtifactory(context);
	}

}
