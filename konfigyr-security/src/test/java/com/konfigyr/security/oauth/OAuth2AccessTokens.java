package com.konfigyr.security.oauth;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Duration;
import java.time.Instant;

public interface OAuth2AccessTokens {

	static OAuth2AccessToken createAccessToken(@NonNull String value) {
		return createAccessToken(value, Duration.ofSeconds(60));
	}

	static OAuth2AccessToken createAccessToken(@NonNull String value, @NonNull Duration expiry) {
		final Instant timestamp = Instant.now();

		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, timestamp, timestamp.plus(expiry));
	}

	static OAuth2RefreshToken createRefreshToken(@NonNull String value) {
		return createRefreshToken(value, Duration.ofSeconds(60));
	}

	static OAuth2RefreshToken createRefreshToken(@NonNull String value, @NonNull Duration expiry) {
		final Instant timestamp = Instant.now();

		return new OAuth2RefreshToken(value, timestamp, timestamp.plus(expiry));
	}

}
