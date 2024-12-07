package com.konfigyr.security;

import com.konfigyr.account.AccountManager;
import com.konfigyr.crypto.*;
import com.konfigyr.namespace.NamespaceManager;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityAutoConfigurationTest {

	final Configurations configurations = AutoConfigurations.of(
			SecurityAutoConfiguration.class,
			RestTemplateAutoConfiguration.class
	);

	ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner().withConfiguration(configurations)
				.withBean(DSLContext.class, () -> mock(DSLContext.class))
				.withBean(AccountManager.class, () -> mock(AccountManager.class))
				.withBean(NamespaceManager.class, () -> mock(NamespaceManager.class))
				.withBean(KeysetOperationsFactory.class, () -> mock(KeysetOperationsFactory.class))
				.withBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class));
	}

	@Test
	void shouldCreateDefaultBeans() {
		runner.withConfiguration(configurations).run(ctx -> {
			assertThat(ctx)
					.hasNotFailed()
					.hasBean("accountPrincipalService")
					.hasBean("principalAccountOAuth2UserService")
					.hasBean("persistentAuthorizedClientService")
					.hasBean("konfigyrWebSecurityExpressionHandler")
					.hasBean("konfigyrMethodSecurityExpressionHandler")
					.hasBean("konfigyrSecurityCustomizer")
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
							.hasBean("persistentAuthorizedClientService")
							.hasBean("konfigyrWebSecurityExpressionHandler")
							.hasBean("konfigyrMethodSecurityExpressionHandler")
							.hasBean("konfigyrSecurityCustomizer");

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
							.hasBean("persistentAuthorizedClientService")
							.hasBean("konfigyrWebSecurityExpressionHandler")
							.hasBean("konfigyrMethodSecurityExpressionHandler")
							.hasBean("konfigyrSecurityCustomizer");

					assertThat(ctx.getBean(PrincipalService.class))
							.isInstanceOf(AccountPrincipalService.class)
							.extracting("cache")
							.isInstanceOf(SpringCacheBasedUserCache.class)
							.extracting("cache")
							.isEqualTo(cache);
				});
	}

}
