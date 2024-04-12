package com.konfigyr.security;

import com.konfigyr.account.AccountManager;
import com.konfigyr.crypto.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityAutoConfigurationTest {

	final Configurations configurations = AutoConfigurations.of(
			SecurityAutoConfiguration.class,
			RestTemplateAutoConfiguration.class
	);

	@Mock
	DSLContext context;

	@Mock
	AccountManager manager;

	@Mock
	KeysetOperationsFactory operationsFactory;

	@Mock
	ClientRegistrationRepository registrationRepository;

	ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner().withConfiguration(configurations)
				.withBean(DSLContext.class, () -> context)
				.withBean(AccountManager.class, () -> manager)
				.withBean(KeysetOperationsFactory.class, () -> operationsFactory)
				.withBean(ClientRegistrationRepository.class, () -> registrationRepository);
	}

	@Test
	void shouldCreateDefaultBeans() {
		runner.withConfiguration(configurations).run(ctx -> {
			assertThat(ctx)
					.hasNotFailed()
					.hasBean("accountPrincipalService")
					.hasBean("principalAccountOAuth2UserService")
					.hasBean("persistentAuthorizedClientService")
					.doesNotHaveBean("userCache");

			assertThat(ctx.getBean(PrincipalService.class))
					.isInstanceOf(AccountPrincipalService.class)
					.extracting("cache")
					.isInstanceOf(NullUserCache.class);
		});
	}

	@Test
	void shouldCreateNoopCachedAccountService() {
		runner.withConfiguration(configurations)
				.withBean(CacheManager.class, SimpleCacheManager::new)
				.run(ctx -> {
					assertThat(ctx)
							.hasNotFailed()
							.hasBean("userCache")
							.hasBean("accountPrincipalService")
							.hasBean("principalAccountOAuth2UserService")
							.hasBean("persistentAuthorizedClientService");

					assertThat(ctx.getBean(PrincipalService.class))
							.isInstanceOf(AccountPrincipalService.class)
							.extracting("cache")
							.isInstanceOf(SpringCacheBasedUserCache.class)
							.extracting("cache")
							.isInstanceOf(NoOpCache.class);
				});
	}

	@Test
	void shouldCreateCachedAccountService() {
		final var cache = new ConcurrentMapCache("user-cache");
		final var manager = new SimpleCacheManager();
		manager.setCaches(List.of(cache));

		runner.withConfiguration(configurations)
				.withBean(CacheManager.class, () -> manager)
				.run(ctx -> {
					assertThat(ctx)
							.hasNotFailed()
							.hasBean("userCache")
							.hasBean("accountPrincipalService")
							.hasBean("principalAccountOAuth2UserService")
							.hasBean("persistentAuthorizedClientService");

					assertThat(ctx.getBean(PrincipalService.class))
							.isInstanceOf(AccountPrincipalService.class)
							.extracting("cache")
							.isInstanceOf(SpringCacheBasedUserCache.class)
							.extracting("cache")
							.isEqualTo(cache);
				});
	}

}