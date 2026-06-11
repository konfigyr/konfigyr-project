package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Locale;

@NullMarked
final class RegisteredClientSettings {

	private static final PropertyMapper mapper = PropertyMapper.get();

	private RegisteredClientSettings() {
	}

	static PropertyMapper mapper() {
		return mapper;
	}

	static ClientSettings.Builder createClientSettings() {
		return ClientSettings.builder()
				.requireProofKey(true)
				.requireAuthorizationConsent(false);
	}

	static ClientSettings.Builder createClientSettings(NamespaceClientId clientId) {
		return createClientSettings()
				.setting(NamespaceClientSettingNames.NAMESPACE, clientId.namespace())
				.setting(NamespaceClientSettingNames.CLIENT_TYPE, clientId.type());
	}

	static TokenSettings.Builder createTokenSettings(AuthorizationProperties properties) {
		final OAuth2AuthorizationServerProperties.Token token = properties.getToken();
		Assert.notNull(token, "Token settings cannot be null");

		// provide Konfigyr default token settings
		final TokenSettings.Builder builder = TokenSettings.builder()
				.authorizationCodeTimeToLive(Duration.ofMinutes(10))
				.accessTokenTimeToLive(Duration.ofMinutes(10))
				.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
				.deviceCodeTimeToLive(Duration.ofMinutes(10))
				.reuseRefreshTokens(false)
				.refreshTokenTimeToLive(Duration.ofHours(1))
				.idTokenSignatureAlgorithm(SignatureAlgorithm.PS256)
				.x509CertificateBoundAccessTokens(false);

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

	static TokenSettings.Builder createTokenSettings(AuthorizationProperties properties, NamespaceClientType type) {
		final TokenSettings.Builder builder = createTokenSettings(properties);
		final AuthorizationProperties.NamespaceTokenSettings settings = properties.getNamespaceTokenSettings().get(type);

		if (settings != null) {
			mapper.from(settings::getAccessTokenTimeToLive).to(builder::accessTokenTimeToLive);
			mapper.from(settings::getRefreshTokenTimeToLive).to(builder::refreshTokenTimeToLive);
		}

		// make sure that refresh tokens are not reused for namespace clients
		return builder.reuseRefreshTokens(false);
	}

}
