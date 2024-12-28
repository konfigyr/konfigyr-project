package com.konfigyr.identity.authentication;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityListener;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityRepository;
import com.konfigyr.identity.authentication.oauth.AuthorizedClientService;
import com.konfigyr.mail.Mailer;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
public class AuthenticationConfiguration {

	private final Lazy<RestOperations> operations;

	public AuthenticationConfiguration(ObjectProvider<RestTemplateBuilder> builder) {
		this.operations = Lazy.of(() -> createRestOperations(builder.getIfAvailable(RestTemplateBuilder::new)));
	}

	@Bean
	UserCache userCache() {
		final Cache<Object, Object> cache = Caffeine.newBuilder()
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.build();

		return new SpringCacheBasedUserCache(
				new CaffeineCache("user-cache", cache, false)
		);
	}

	@Bean
	AccountIdentityRepository accountIdentityRepository(DSLContext context, ApplicationEventPublisher publisher) {
		return new AccountIdentityRepository(context, publisher);
	}

	@Bean
	AccountIdentityService accountIdentityService(AccountIdentityRepository repository, UserCache cache) {
		return new DefaultAccountIdentityService(repository, cache);
	}

	@Bean
	OAuth2UserService<OAuth2UserRequest, OAuth2User> identityUserService(AccountIdentityService service) {
		final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		delegate.setRestOperations(operations.get());

		return request -> service.get(delegate, request);
	}

	@Bean
	OAuth2AuthorizedClientService accountIdentityAuthorizedClientService(
			DSLContext context,
			KeysetOperationsFactory keysetOperationsFactory,
			ClientRegistrationRepository clientRegistrationRepository) {
		final KeysetOperations operations = keysetOperationsFactory.create(KonfigyrIdentityKeysets.AUTHORIZED_CLIENTS);
		return new AuthorizedClientService(context, operations, clientRegistrationRepository);
	}

	@Bean
	AccountIdentityListener accountIdentityListener(Mailer mailer) {
		return new AccountIdentityListener(mailer);
	}

	static RestOperations createRestOperations(@NonNull RestTemplateBuilder builder) {
		return builder.errorHandler(new OAuth2ErrorResponseErrorHandler()).build();
	}

}
