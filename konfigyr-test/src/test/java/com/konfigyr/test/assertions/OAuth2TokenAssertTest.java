package com.konfigyr.test.assertions;

import com.konfigyr.test.OAuth2AccessTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class OAuth2TokenAssertTest {

	@Test
	@DisplayName("should assert Refresh Token")
	void shouldAssertRefreshToken() {
		final var token = OAuth2AccessTokens.createRefreshToken("value", Duration.ofHours(8));

		assertThat(token)
				.asInstanceOf(OAuth2TokenAssert.factory())
				.hasValue("value")
				.issuedAt(token.getIssuedAt())
				.issuedAt(Instant.now(), within(300, ChronoUnit.MILLIS))
				.expiresAt(token.getExpiresAt())
				.expiresAt(Instant.now().plus(8, ChronoUnit.HOURS), within(300, ChronoUnit.MILLIS));

		assertThatThrownBy(() -> OAuth2TokenAssert.assertThat(token).hasValue("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that OAuth 2.0 Token should have a value of \"invalid\" but was \"value\"");

		assertThatThrownBy(() -> OAuth2TokenAssert.assertThat(token).issuedAt(Instant.now()))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that OAuth 2.0 Token should be issued at");

		assertThatThrownBy(() -> OAuth2TokenAssert.assertThat(token).expiresAt(Instant.now()))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that OAuth 2.0 Token should expire at");
	}

	@Test
	@DisplayName("should assert Access Token")
	void shouldAssertAccessToken() {
		final var token = OAuth2AccessTokens.createAccessToken("value", Duration.ofHours(12), "profile", "email");

		assertThat(token)
				.asInstanceOf(OAuth2AccessTokenAssert.factory())
				.isBearerToken()
				.hasValue("value")
				.containsScopes("profile")
				.containsScopes("email")
				.hasScopes("profile", "email")
				.doesNotContainScopes("unknown")
				.issuedAt(token.getIssuedAt())
				.issuedAt(Instant.now(), within(300, ChronoUnit.MILLIS))
				.expiresAt(token.getExpiresAt())
				.expiresAt(Instant.now().plus(12, ChronoUnit.HOURS), within(300, ChronoUnit.MILLIS));

		assertThatThrownBy(() -> OAuth2AccessTokenAssert.assertThat(token).containsScopes("unknown"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that OAuth 2.0 Access Token should contain scopes");

		assertThatThrownBy(() -> OAuth2AccessTokenAssert.assertThat(token).hasScopes("profile", "unknown"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that OAuth 2.0 Access Token should contain all scopes");
	}

}
