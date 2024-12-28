package com.konfigyr.identity.authorization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;

import static org.assertj.core.api.Assertions.*;

class AuthorizedScopesTest {

	@Test
	@DisplayName("should generate authorized scopes when no previous consent was given")
	void shouldGenerateAuthorizedScopesWhenNoPreviousConsentGiven() {
		assertThat(AuthorizedScope.from("openid profile email", null))
				.hasSize(2)
				.containsExactlyInAnyOrder(
						AuthorizedScope.unauthorized("profile"),
						AuthorizedScope.unauthorized("email")
				);
	}

	@Test
	@DisplayName("should generate authorized scopes when consent was given")
	void shouldGenerateAuthorizedScopesWhenConsentGiven() {
		final var consent = OAuth2AuthorizationConsent.withId("client", "john.doe")
						.scope("profile")
						.build();

		assertThat(AuthorizedScope.from("openid profile email", consent))
				.hasSize(2)
				.containsExactlyInAnyOrder(
						AuthorizedScope.authorized("profile"),
						AuthorizedScope.unauthorized("email")
				);
	}

}
