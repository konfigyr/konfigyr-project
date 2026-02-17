package com.konfigyr.vault;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@RequiredArgsConstructor
public class VaultAutoConfiguration {

	private final DSLContext context;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean
	ProfileManager defaultProfileManager() {
		return new DefaultProfileManager(context, applicationEventPublisher);
	}

}
