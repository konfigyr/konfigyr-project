package com.konfigyr.identity.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.konfigyr.identity.authorization.jwk.KeyRepository;
import com.konfigyr.identity.authorization.jwk.RepositoryKeySource;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jooq.DSLContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.util.Assert;

import java.security.Security;

@Configuration(proxyBeanMethods = false)
public class AuthorizationConfiguration implements InitializingBean {

	private final Lazy<ObjectMapper> mapper;

	public AuthorizationConfiguration(ResourceLoader resourceLoader) {
		this.mapper = Lazy.of(() -> createObjectMapper(resourceLoader.getClassLoader()));
	}

	@Override
	public void afterPropertiesSet() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Bean
	AuthorizationService authorizationService(
			DSLContext context, KeysetOperationsFactory keysetOperationsFactory,
			ApplicationEventPublisher applicationEventPublisher, RegisteredClientRepository registeredClientRepository) {
		return new DefaultAuthorizationService(context, applicationEventPublisher, mapper.get(),
				keysetOperationsFactory.create(KonfigyrIdentityKeysets.AUTHORIZATIONS), registeredClientRepository);
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder()
				.authorizationEndpoint("/oauth/authorize")
				.tokenEndpoint("/oauth/token")
				.jwkSetEndpoint("/oauth/jwks")
				.tokenIntrospectionEndpoint("/oauth/introspect")
				.tokenRevocationEndpoint("/oauth/revoke")
				.oidcUserInfoEndpoint("/oauth/userinfo")
				.build();
	}

	@Bean
	KeyRepository keyRepository(DSLContext context, KeysetOperationsFactory keysetOperationsFactory) {
		return new KeyRepository(context, keysetOperationsFactory.create(KonfigyrIdentityKeysets.WEB_KEYS));
	}

	@Bean
	JWKSource<SecurityContext> repositoryKeySource(KeyRepository repository) {
		return new RepositoryKeySource(repository);
	}

	@NonNull
	static ObjectMapper createObjectMapper(ClassLoader classLoader) {
		Assert.notNull(classLoader, "Class loader must not be null");

		return new ObjectMapper()
				.registerModules(SecurityJackson2Modules.getModules(classLoader))
				.registerModule(new OAuth2AuthorizationServerJackson2Module());
	}

}
