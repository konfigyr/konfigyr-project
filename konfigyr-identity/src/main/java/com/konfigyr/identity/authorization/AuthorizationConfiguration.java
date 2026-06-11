package com.konfigyr.identity.authorization;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.crypto.KeysetStore;
import com.konfigyr.crypto.jose.JoseAutoConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authorization.client.CachingRegisteredClientRepository;
import com.konfigyr.identity.authorization.client.DelegatingRegisteredClientRepository;
import com.konfigyr.identity.authorization.client.KonfigyrRegisteredClientRepository;
import com.konfigyr.identity.authorization.client.RegisteredNamespaceClientRepository;
import com.konfigyr.identity.authorization.issuer.CompositeTrustedIssuerRepository;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRepository;
import com.konfigyr.identity.authorization.issuer.WellKnownTrustedIssuers;
import com.konfigyr.identity.authorization.jwk.KeysetSource;
import com.konfigyr.identity.authorization.jwk.SigningJwkSelector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.util.Lazy;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.security.Security;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(JoseAutoConfiguration.class)
@EnableConfigurationProperties(AuthorizationProperties.class)
public class AuthorizationConfiguration implements InitializingBean {

	private final Lazy<@NonNull ObjectMapper> mapper;
	private final AuthorizationProperties properties;

	public AuthorizationConfiguration(ResourceLoader resourceLoader, AuthorizationProperties properties) {
		this.mapper = Lazy.of(() -> createObjectMapper(resourceLoader.getClassLoader()));
		this.properties = properties;
	}

	@Override
	public void afterPropertiesSet() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Bean
	AuthorizationService authorizationService(
			DSLContext context, KeysetOperationsFactory keysetOperationsFactory,
			ApplicationEventPublisher applicationEventPublisher, RegisteredClientRepository registeredClientRepository) {
		return new DefaultAuthorizationService(context, applicationEventPublisher, mapper.get(),
				keysetOperationsFactory.create(KonfigyrIdentityKeysets.AUTHORIZATIONS), registeredClientRepository);
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(DSLContext context, JsonMapper jsonMapper, CacheMetricsRegistrar registrar) {
		Assert.notNull(properties.getCache().getSpec(), "Cache specification must not be null");

		final CaffeineCache cache = new CaffeineCache(
				"identity.registered-clients",
				Caffeine.from(properties.getCache().getSpec()).recordStats().build()
		);

		registrar.bindCacheToRegistry(cache);

		return new DelegatingRegisteredClientRepository(List.of(
				new KonfigyrRegisteredClientRepository(properties),
				new CachingRegisteredClientRepository(cache, new RegisteredNamespaceClientRepository(properties, context, jsonMapper))
		));
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
				.multipleIssuersAllowed(false)
				.build();
	}

	@Bean
	KeysetSource identityServerKeysetSource(KeysetStore store) {
		return new KeysetSource(store, KonfigyrIdentityKeysets.WEB_KEYS);
	}

	@Bean
	NimbusJwtEncoder identityServerJwtEncoder(KeysetSource identityServerKeysetSource) {
		final NimbusJwtEncoder encoder = new NimbusJwtEncoder(identityServerKeysetSource);
		encoder.setJwkSelector(SigningJwkSelector.getInstance()::select);
		return encoder;
	}

	@Bean
	NamespaceMembershipValidator namespaceMembershipValidator(DSLContext context) {
		return new NamespaceMembershipValidator(context);
	}

	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
		return new TokenCustomizer(
				properties.getAudiences()
						.stream()
						.filter(StringUtils::hasText)
						.toList()
		);
	}

	@Bean
	TrustedIssuerRepository trustedIssuerRepository() {
		return new CompositeTrustedIssuerRepository(
				WellKnownTrustedIssuers.getInstance()
		);
	}

	@NonNull
	static ObjectMapper createObjectMapper(ClassLoader classLoader) {
		Assert.notNull(classLoader, "Class loader must not be null");

		final BasicPolymorphicTypeValidator.Builder validator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(AccountIdentity.class)
				.allowIfSubType(EntityId.class);

		return JsonMapper.builder()
				.addModules(SecurityJacksonModules.getModules(classLoader, validator))
				.build();
	}

}
