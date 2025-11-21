package com.konfigyr.test;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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
	 * Creates a new {@link OidcIdToken} with the given token value and JWT Claims. The expiration duration for
	 * the created token is <code>60 seconds</code>.
	 *
	 * @param value the OIDC ID Token value, can't be {@literal null}
	 * @param claims JWT claims to be added to the token, can't be {@literal null}
	 * @return the OIDC ID Token, never {@literal null}
	 */
	@NonNull
	static OidcIdToken createIdToken(@NonNull String value, @NonNull Map<String, Object> claims) {
		return createIdToken(value, Duration.ofSeconds(60), claims);
	}

	/**
	 * Creates a new {@link OidcIdToken} with the given token value, expiration duration and JWT claims.
	 *
	 * @param value the OIDC ID Token value, can't be {@literal null}
	 * @param expiry expiration duration, can't be {@literal null}
	 * @param claims JWT claims to be added to the token, can't be {@literal null}
	 * @return the OIDC ID Token, never {@literal null}
	 */
	@NonNull
	static OidcIdToken createIdToken(@NonNull String value, @NonNull Duration expiry, @NonNull Map<String, Object> claims) {
		final Instant timestamp = Instant.now();
		return new OidcIdToken(value, timestamp, timestamp.plus(expiry), claims);
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
