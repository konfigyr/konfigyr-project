package com.konfigyr.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.Configurations;
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
				.hasCauseInstanceOf(BeanInstantiationException.class)
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasRootCauseMessage("Failed to resolve Key Encryption Key (KEK), no shares or value specified")
		);
	}

	@Test
	@DisplayName("should setup context without keyset cache")
	void shouldCreateContextWithoutKeysetCache() {
		runner.withPropertyValues(
				"konfigyr.crypto.cache=false",
				"konfigyr.crypto.master-key.value=c7miwShcEQkZUcNQGqliVA=="
		).run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.hasBean("konfigyrKeysetOperationsFactory")
				.hasBean("konfigyrKekProvider")
				.doesNotHaveBean("registryKeysetCache")
		);
	}

	@Test
	@DisplayName("should setup context with master key defined as shares")
	void shouldCreateContextWithShares() {
		runner.withPropertyValues(
				"konfigyr.crypto.cache=false",
				"konfigyr.crypto.master-key.shares[0]=AAAAAeBidNgpLYqxb7yvarNYOoYF4hlfUvvpeAQ9D-jQfRoc",
				"konfigyr.crypto.master-key.shares[1]=AAAAArutxHdXromUjZTDugLPGH6zZJoBSgXb7bw0mGU4gHRu",
				"konfigyr.crypto.master-key.shares[2]=AAAAA1-YWR3Dc8h40fykrtxy_hOs66v_9aIDtJ9oWD8gJ7eW"
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
				"konfigyr.crypto.master-key.value=c7miwShcEQkZUcNQGqliVA=="
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
				"konfigyr.crypto.master-key.value=meITChyITJK_Z46MKXNN2LeSYJQdb1aGaLAWoy9-g3Y="
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
