package com.konfigyr.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration used to register the {@link KeysetOperationsFactory} Spring Bean that is used
 * to resolve {@link KeysetOperations}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
@ConditionalOnBean(KeysetStore.class)
@AutoConfigureAfter(CryptoAutoConfiguration.class)
@ConditionalOnMissingBean(KeysetOperationsFactory.class)
public class KeysetOperationsAutoConfiguration {

	@Bean
	KeysetOperationsFactory konfigyrKeysetOperationsFactory(KeysetStore store) {
		return new KonfigyrKeysetOperationsFactory(store);
	}

}
