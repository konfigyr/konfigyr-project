package com.konfigyr.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CryptographyAutoConfigurationTest {

	final Configurations configurations = AutoConfigurations.of(
			CryptoAutoConfiguration.class,
			CryptographyAutoConfiguration.class
	);

	ApplicationContextRunner runner;

	@Mock
	KeysetFactory keysetFactory;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner()
				.withConfiguration(configurations)
				.withBean(KeysetFactory.class, () -> keysetFactory);
	}

	@Test
	@DisplayName("should validate crypto properties")
	void shouldValidateCryptoProperties() {
		runner.run(ctx -> assertThat(ctx)
				.hasFailed()
				.getFailure()
				.hasCauseInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
		);
	}

	@Test
	@DisplayName("should setup context without keyset cache")
	void shouldCreateContextWithoutKeysetCache() {
		runner.withPropertyValues(
				"konfigyr.crypto.cache=false",
				"konfigyr.crypto.master-key=c7miwShcEQkZUcNQGqliVA=="
		).run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.hasBean("konfigyrKeysetOperationsFactory")
				.hasBean("konfigyrKekProvider")
				.doesNotHaveBean("registryKeysetCache")
		);
	}

	@Test
	@DisplayName("should setup context without keyset cache as cache manager is not present")
	void shouldCreateContextWithoutCacheManager() {
		runner.withPropertyValues(
				"konfigyr.crypto.cache=true",
				"konfigyr.crypto.master-key=c7miwShcEQkZUcNQGqliVA=="
		).run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.hasBean("konfigyrKeysetOperationsFactory")
				.hasBean("konfigyrKekProvider")
				.doesNotHaveBean("registryKeysetCache")
		);
	}

	@Test
	@DisplayName("should setup context with keyset cache")
	void shouldCreateContextWithKeysetCache() {
		runner.withPropertyValues(
				"konfigyr.crypto.master-key=c7miwShcEQkZUcNQGqliVA=="
		).withBean(
				CacheManager.class, NoOpCacheManager::new
		).run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.hasBean("konfigyrKeysetOperationsFactory")
				.hasBean("konfigyrKekProvider")
				.hasBean("registryKeysetCache")
		);
	}

}
