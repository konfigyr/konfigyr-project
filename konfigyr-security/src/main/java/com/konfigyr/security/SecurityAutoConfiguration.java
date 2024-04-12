package com.konfigyr.security;

import com.konfigyr.account.AccountManager;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.security.oauth.AuthorizedClientService;
import com.konfigyr.security.oauth.PrincipalAccountOAuth2UserService;
import com.konfigyr.security.oauth.OAuthKeysets;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Spring autoconfiguration that would register required OAuth Client Spring Beans to interact with
 * Konfigyr user accounts and their access tokens that would be used with Spring Security.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
public class SecurityAutoConfiguration {

	@Bean
	@ConditionalOnBean(CacheManager.class)
	@ConditionalOnMissingBean(UserCache.class)
	UserCache userCache(CacheManager manager) {
		Cache cache = manager.getCache("user-cache");
		if (cache == null) {
			cache = new NoOpCache("user-cache");
		}

		return new SpringCacheBasedUserCache(cache);
	}

	@Bean
	@ConditionalOnBean(UserCache.class)
	PrincipalService cachingAccountPrincipalService(AccountManager accountManager, UserCache userCache) {
		return new AccountPrincipalService(accountManager, userCache);
	}

	@Bean
	@ConditionalOnMissingBean(PrincipalService.class)
	PrincipalService accountPrincipalService(AccountManager accountManager) {
		return new AccountPrincipalService(accountManager);
	}

	@Bean
	OAuth2UserService<OAuth2UserRequest, OAuth2User> principalAccountOAuth2UserService(
			PrincipalService principalService,
			RestTemplateBuilder restTemplateBuilder) {
		return new PrincipalAccountOAuth2UserService(principalService, restTemplateBuilder);
	}

	@Bean
	OAuth2AuthorizedClientService persistentAuthorizedClientService(
			DSLContext context,
			KeysetOperationsFactory keysetOperationsFactory,
			ClientRegistrationRepository clientRegistrationRepository) {
		return new AuthorizedClientService(context, keysetOperationsFactory.create(OAuthKeysets.ACCESS_TOKEN),
				clientRegistrationRepository);
	}

}
