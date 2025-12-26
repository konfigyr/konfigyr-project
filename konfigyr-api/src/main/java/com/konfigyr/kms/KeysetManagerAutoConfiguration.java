package com.konfigyr.kms;

import com.konfigyr.crypto.KeysetStore;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@RequiredArgsConstructor
public class KeysetManagerAutoConfiguration {

	private final DSLContext context;

	@Bean
	@ConditionalOnMissingBean(KeysetManager.class)
	KeysetManager defaultKeysetManager(ApplicationEventPublisher eventPublisher, KeysetStore keysetStore) {
		return new DefaultKeysetManager(context, keysetStore, eventPublisher);
	}

}
