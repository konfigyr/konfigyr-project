package com.konfigyr.security;

import com.konfigyr.account.AccountManager;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.security.oauth.AuthorizedClientService;
import com.konfigyr.security.oauth.DatabaseOAuth2UserService;
import com.konfigyr.security.oauth.OAuthKeysets;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
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
	OAuth2UserService<OAuth2UserRequest, OAuth2User> databaseOAuth2UserService(
			AccountManager accountManager,
			RestTemplateBuilder restTemplateBuilder) {
		return new DatabaseOAuth2UserService(accountManager, restTemplateBuilder);
	}

	@Bean
	OAuth2AuthorizedClientService authorizedClientService(
			DSLContext context,
			KeysetOperationsFactory keysetOperationsFactory,
			ClientRegistrationRepository clientRegistrationRepository) {
		return new AuthorizedClientService(context, keysetOperationsFactory.create(OAuthKeysets.ACCESS_TOKEN),
				clientRegistrationRepository);
	}

}
