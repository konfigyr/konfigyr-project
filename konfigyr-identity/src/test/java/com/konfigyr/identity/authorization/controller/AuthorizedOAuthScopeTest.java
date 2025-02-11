package com.konfigyr.identity.authorization.controller;

import com.konfigyr.security.OAuthScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;

import static org.assertj.core.api.Assertions.*;

class AuthorizedOAuthScopeTest {

	@Test
	@DisplayName("should generate authorized scopes when no previous consent was given")
	void shouldGenerateAuthorizedScopesWhenNoPreviousConsentGiven() {
		assertThat(AuthorizedScope.from("openid namespaces:read namespaces:write", null))
				.hasSize(2)
				.containsExactlyInAnyOrder(
						AuthorizedScope.unauthorized(OAuthScope.READ_NAMESPACES),
						AuthorizedScope.unauthorized(OAuthScope.WRITE_NAMESPACES)
				);
	}

	@Test
	@DisplayName("should generate authorized scopes when consent was given")
	void shouldGenerateAuthorizedScopesWhenConsentGiven() {
		final var consent = OAuth2AuthorizationConsent.withId("client", "john.doe")
						.scope("namespaces:read")
						.build();

		assertThat(AuthorizedScope.from("openid namespaces:read namespaces:write", consent))
				.hasSize(2)
				.containsExactlyInAnyOrder(
						AuthorizedScope.authorized(OAuthScope.READ_NAMESPACES),
						AuthorizedScope.unauthorized(OAuthScope.WRITE_NAMESPACES)
				);
	}

	@Test
	@DisplayName("should create authorized scopes")
	void shouldCreateAuthorizedScope() {
		assertThat(AuthorizedScope.authorized(OAuthScope.READ_NAMESPACES))
				.isNotNull()
				.returns(OAuthScope.READ_NAMESPACES, AuthorizedScope::scope)
				.returns(true, AuthorizedScope::authorized)
				.returns(OAuthScope.READ_NAMESPACES.getAuthority(), AuthorizedScope::value)
				.returns("konfigyr.oauth.scope.READ_NAMESPACES", AuthorizedScope::messageKey);
	}

	@Test
	@DisplayName("should create unauthorized scopes")
	void shouldCreateUnauthorizedScope() {
		assertThat(AuthorizedScope.unauthorized(OAuthScope.DELETE_NAMESPACES))
				.isNotNull()
				.returns(OAuthScope.DELETE_NAMESPACES, AuthorizedScope::scope)
				.returns(false, AuthorizedScope::authorized)
				.returns(OAuthScope.DELETE_NAMESPACES.getAuthority(), AuthorizedScope::value)
				.returns("konfigyr.oauth.scope.DELETE_NAMESPACES", AuthorizedScope::messageKey);
	}

}
