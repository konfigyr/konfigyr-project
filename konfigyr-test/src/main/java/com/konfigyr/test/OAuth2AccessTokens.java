package com.konfigyr.test;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Utility class used to generate various {@link org.springframework.security.oauth2.core.OAuth2Token OAuth2 Tokens}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface OAuth2AccessTokens {

	/**
	 * Creates a new {@link OAuth2AccessToken} with the given token value. The expiration duration for
	 * the created token is <code>60 seconds</code>.
	 *
	 * @param value the OAuth2 Access Token value, can't be {@literal null}
	 * @param scopes scopes to be added to the token
	 * @return the OAuth2 Access Token, never {@literal null}
	 */
	@NonNull
	static OAuth2AccessToken createAccessToken(@NonNull String value, String... scopes) {
		return createAccessToken(value, Duration.ofSeconds(60), scopes);
	}

	/**
	 * Creates a new {@link OAuth2AccessToken} with the given token value and expiration duration.
	 *
	 * @param value the OAuth2 Access Token value, can't be {@literal null}
	 * @param expiry expiration duration, can't be {@literal null}
	 * @param scopes scopes to be added to the token
	 * @return the OAuth2 Access Token, never {@literal null}
	 */
	@NonNull
	static OAuth2AccessToken createAccessToken(@NonNull String value, @NonNull Duration expiry, String... scopes) {
		final Instant timestamp = Instant.now();
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, value, timestamp,
				timestamp.plus(expiry), Set.of(scopes));
	}

	/**
	 * Creates a new {@link OAuth2RefreshToken} with the given token value. The expiration duration for
	 * the created token is <code>60 seconds</code>.
	 *
	 * @param value the OAuth2 Refresh Token value, can't be {@literal null}
	 * @return the OAuth2 Refresh Token, never {@literal null}
	 */
	@NonNull
	static OAuth2RefreshToken createRefreshToken(@NonNull String value) {
		return createRefreshToken(value, Duration.ofSeconds(60));
	}

	/**
	 * Creates a new {@link OAuth2AccessToken} with the given token value and expiration duration.
	 *
	 * @param value the OAuth2 Refresh Token value, can't be {@literal null}
	 * @param expiry expiration duration, can't be {@literal null}
	 * @return the OAuth2 Refresh Token, never {@literal null}
	 */
	@NonNull
	static OAuth2RefreshToken createRefreshToken(@NonNull String value, @NonNull Duration expiry) {
		final Instant timestamp = Instant.now();

		return new OAuth2RefreshToken(value, timestamp, timestamp.plus(expiry));
	}

}
