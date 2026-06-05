package com.konfigyr.mcp;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.security.oauth.AuthenticatedPrincipalAuthenticationToken;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpNamespaceContextTest {

	@Mock
	NamespaceManager namespaceManager;

	NamespaceContext context;

	@BeforeEach
	void setup() {
		context = new McpNamespaceContext(namespaceManager);
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("should resolve namespace from JWT subject and cache it within the same request scope instance")
	void shouldCacheResolutionWithinRequestScope() {
		final Namespace expected = mock(Namespace.class);

		when(namespaceManager.findNamespaceByClientId("kfg-test-client"))
				.thenReturn(Optional.of(expected));

		SecurityContextHolder.getContext().setAuthentication(oauthClientAuthentication("kfg-test-client"));

		assertThat(context.resolve())
				.isSameAs(context.resolve())
				.isSameAs(expected);

		verify(namespaceManager).findNamespaceByClientId("kfg-test-client");
	}

	@Test
	@DisplayName("throws AccessDeniedException when the client ID does not match any application")
	void shouldThrowWhenClientIdIsUnknown() {
		when(namespaceManager.findNamespaceByClientId("kfg-ghost"))
				.thenReturn(Optional.empty());

		SecurityContextHolder.getContext().setAuthentication(oauthClientAuthentication("kfg-ghost"));

		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(context::resolve)
				.withMessage("No active namespace application found for client: '%s'", "kfg-ghost");
	}

	@Test
	@DisplayName("throws AccessDeniedException for unsupported principal type")
	void shouldThrowForUserAccountPrincipal() {
		SecurityContextHolder.getContext().setAuthentication(TestPrincipals.john());

		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(context::resolve)
				.withMessage("Namespace cannot be resolved for '%s' principal type", PrincipalType.USER_ACCOUNT);

		verifyNoInteractions(namespaceManager);
	}

	@Test
	@DisplayName("throws AuthenticationCredentialsNotFoundException when there is no authentication in the security context")
	void shouldThrowWhenNotAuthenticated() {
		assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
				.isThrownBy(context::resolve);

		verifyNoInteractions(namespaceManager);
	}

	private static AuthenticatedPrincipalAuthenticationToken oauthClientAuthentication(String clientId) {
		final Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "RS256")
				.claim("sub", clientId)
				.claim(OAuth2ParameterNames.SCOPE, OAuthScopes.of(OAuthScope.READ_NAMESPACES).toString())
				.issuer("http://test-issuer")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();

		return AuthenticatedPrincipalAuthenticationToken.of(jwt);
	}

}
