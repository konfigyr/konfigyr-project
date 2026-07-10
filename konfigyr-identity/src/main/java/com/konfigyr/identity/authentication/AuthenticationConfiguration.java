package com.konfigyr.identity.authentication;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityListener;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityRepository;
import com.konfigyr.identity.authentication.oauth.AuthorizedClientService;
import com.konfigyr.identity.authentication.rememberme.AccountRememberMeServices;
import com.konfigyr.mail.Mailer;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Lazy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.cache.SpringCacheBasedUserCache;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestOperations;

import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties.class)
public class AuthenticationConfiguration {

	private final AuthenticationProperties properties;
	private final Lazy<@NonNull DefaultOAuth2UserService> oauthUserService;

	public AuthenticationConfiguration(
			AuthenticationProperties properties,
			ObjectProvider<@NonNull RestTemplateBuilder> builder
	) {
		this.properties = properties;
		this.oauthUserService = Lazy.of(() -> createUserService(builder.getIfAvailable(RestTemplateBuilder::new)));
	}

	@Bean
	UserCache userCache() {
		final Cache<@NonNull Object, Object> cache = Caffeine.newBuilder()
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
	AccountRememberMeServices accountRememberMeServices(AccountIdentityService service) {
		return new AccountRememberMeServices(properties.getRememberMe().getKey(), service::get);
	}

	@Bean
	OAuth2UserService<OAuth2UserRequest, OAuth2User> identityUserService(AccountIdentityService service) {
		final DefaultOAuth2UserService delegate = oauthUserService.get();

		return request -> (OAuth2User) service.get(delegate, request);
	}

	@Bean
	OAuth2UserService<OidcUserRequest, OidcUser> oidcIdentityUserService(AccountIdentityService service) {
		final OidcUserService delegate = new OidcUserService();
		delegate.setOauth2UserService(oauthUserService.get());

		return request -> (OidcUser) service.get(delegate, request);
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
	JwtDecoderFactory<ClientRegistration> oidcTokenDecoderFactory(RestTemplateBuilder builder) {
		return new OidcTokenDecoderFactory(builder.build(), properties.getOidc().getJwtClockSkew());
	}

	@Bean
	AccountIdentityListener accountIdentityListener(Mailer mailer) {
		return new AccountIdentityListener(mailer);
	}

	static DefaultOAuth2UserService createUserService(@NonNull RestTemplateBuilder builder) {
		final RestOperations operations = builder.errorHandler(new OAuth2ErrorResponseErrorHandler())
				.build();

		final DefaultOAuth2UserService service = new DefaultOAuth2UserService();
		service.setRestOperations(operations);

		return service;
	}

	@Configuration(proxyBeanMethods = false)
	static class AccessTokenResponseClientConfiguration {

		private final RestClient restClient;

		AccessTokenResponseClientConfiguration(ClientHttpRequestFactory clientHttpRequestFactory) {
			this.restClient = RestClient.builder()
					.requestFactory(clientHttpRequestFactory)
					.defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
					.configureMessageConverters((messageConverters) -> {
						messageConverters.addCustomConverter(new FormHttpMessageConverter());
						messageConverters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
					}).build();
		}

		@Bean
		OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
			return customize(new RestClientAuthorizationCodeTokenResponseClient());
		}

		@Bean
		OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient() {
			return customize(new RestClientClientCredentialsTokenResponseClient());
		}

		@Bean
		OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenTokenResponseClient() {
			return customize(new RestClientRefreshTokenTokenResponseClient());
		}

		@Bean
		OAuth2AccessTokenResponseClient<TokenExchangeGrantRequest> tokenExchangeTokenResponseClient() {
			return customize(new RestClientTokenExchangeTokenResponseClient());
		}

		@Bean
		OAuth2AccessTokenResponseClient<JwtBearerGrantRequest> jwtBearerTokenResponseClient() {
			return customize(new RestClientJwtBearerTokenResponseClient());
		}

		<T extends AbstractOAuth2AuthorizationGrantRequest> OAuth2AccessTokenResponseClient<T> customize(
				AbstractRestClientOAuth2AccessTokenResponseClient<T> client
		) {
			client.setRestClient(restClient);
			return client;
		}
	}

}
