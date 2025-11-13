package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

abstract class AbstractRegisteredClientRepository implements RegisteredClientRepository {

	private static final Set<ClientAuthenticationMethod> DEFAULT_AUTHENTICATION_METHODS = Set.of(
			ClientAuthenticationMethod.CLIENT_SECRET_BASIC, ClientAuthenticationMethod.CLIENT_SECRET_POST
	);

	static final PropertyMapper mapper = PropertyMapper.get();

	protected final AuthorizationProperties properties;

	AbstractRegisteredClientRepository(AuthorizationProperties properties) {
		Assert.notNull(properties, "Authorization properties cannot be null");
		this.properties = properties;
	}

	@Override
	public final void save(RegisteredClient registeredClient) {
		throw new UnsupportedOperationException("Registering OAuth clients is not supported");
	}

	@NonNull
	protected Collection<ClientAuthenticationMethod> getClientAuthenticationMethods() {
		return DEFAULT_AUTHENTICATION_METHODS;
	}

	@NonNull
	protected abstract Collection<AuthorizationGrantType> getAuthorizationGrantTypes();

	@NonNull
	protected RegisteredClient.Builder createRegisteredClient(String id) {
		Assert.hasText(id, "OAuth client registration identifier cannot be empty");

		final RegisteredClient.Builder builder = RegisteredClient.withId(id);
		getClientAuthenticationMethods().forEach(builder::clientAuthenticationMethod);
		getAuthorizationGrantTypes().forEach(builder::authorizationGrantType);

		properties.getRedirectUris().forEach(uri -> mapper.from(uri)
				.whenHasText()
				.to(builder::redirectUri)
		);

		properties.getPostLogoutRedirectUris().forEach(uri -> mapper.from(uri)
				.whenHasText()
				.to(builder::postLogoutRedirectUri)
		);

		return builder;
	}

	@NonNull
	protected ClientSettings.Builder createClientSettings() {
		return ClientSettings.builder()
				.requireAuthorizationConsent(true)
				.requireProofKey(true);
	}

	@NonNull
	protected TokenSettings.Builder createTokenSettings() {
		final OAuth2AuthorizationServerProperties.Token token = properties.getToken();
		Assert.notNull(token, "Token settings cannot be null");

		final TokenSettings.Builder builder = TokenSettings.builder();
		mapper.from(token::getAuthorizationCodeTimeToLive).to(builder::authorizationCodeTimeToLive);
		mapper.from(token::getAccessTokenTimeToLive).to(builder::accessTokenTimeToLive);
		mapper.from(token::getAccessTokenFormat).as(OAuth2TokenFormat::new).to(builder::accessTokenFormat);
		mapper.from(token::getDeviceCodeTimeToLive).to(builder::deviceCodeTimeToLive);
		mapper.from(token::isReuseRefreshTokens).to(builder::reuseRefreshTokens);
		mapper.from(token::getRefreshTokenTimeToLive).to(builder::refreshTokenTimeToLive);
		mapper.from(token::getIdTokenSignatureAlgorithm)
				.as(algorithm -> SignatureAlgorithm.from(algorithm.toUpperCase(Locale.ROOT)))
				.to(builder::idTokenSignatureAlgorithm);

		return builder;
	}

}
