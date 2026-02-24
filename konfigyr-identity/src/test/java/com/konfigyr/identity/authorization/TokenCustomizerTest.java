package com.konfigyr.identity.authorization;

import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.OAuthAccountIdentityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TokenCustomizerTest {

	final AccountIdentity identity = AccountIdentities.john().build();

	final TokenCustomizer customizer = new TokenCustomizer();

	@Test
	@DisplayName("should not customize ID token when account identity is not present in Authentication")
	void ignoreInvalidAuthenticationTypesForIdToken() {
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, "some principal");

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(1)
				.containsEntry(StandardClaimNames.SUB, "test-subject");
	}

	@Test
	@DisplayName("should not customize OAuth access token when account identity is not present in Authentication")
	void ignoreInvalidAuthenticationTypesForAccessToken() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, "some principal");

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(1)
				.containsEntry(StandardClaimNames.SUB, "test-subject");
	}

	@Test
	@DisplayName("should not customize OAuth Access token when registered client is not present in Authentication")
	void ignoreMissingRegisteredClient() {
		final var authentication = mock(OAuth2ClientAuthenticationToken.class);
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, authentication);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(1)
				.containsEntry(StandardClaimNames.SUB, "test-subject");
	}

	@Test
	@DisplayName("should customize OAuth Access token when registered client is present in Authentication")
	void customizeAccessTokenForClient() {
		final var client = mock(RegisteredClient.class);
		final var authentication = mock(OAuth2ClientAuthenticationToken.class);
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, authentication);

		doReturn("Test client name").when(client).getClientName();
		doReturn(client).when(authentication).getRegisteredClient();

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(2)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.NAME, "Test client name");
	}

	@Test
	@DisplayName("should customize OAuth Access token when account identity is present in Authentication")
	void customizeAccessTokenForIdentity() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, identity);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(3)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName())
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail());
	}

	@Test
	@DisplayName("should customize OAuth Access token when account identity user is present in Authentication")
	void customizeAccessTokenForUser() {
		final var user = new OAuthAccountIdentityUser(identity, mock(OAuth2User.class));
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, user);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(3)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName());
	}

	@Test
	@DisplayName("should customize ID token when account identity is present in Authentication")
	void customizeIdTokenForIdentity() {
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, identity);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(5)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry("oid", identity.getUsername())
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName())
				.containsEntry(StandardClaimNames.PICTURE, identity.getAvatar().get());
	}

	@Test
	@DisplayName("should customize ID token when account identity user is present in Authentication")
	void customizeIdTokenForUser() {
		final var user = new OAuthAccountIdentityUser(identity, mock(OAuth2User.class));
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, user);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(5)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry("oid", identity.getUsername())
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName())
				.containsEntry(StandardClaimNames.PICTURE, identity.getAvatar().get());
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, Object principal) {
		final var authentication = mock(Authentication.class);
		doReturn(principal).when(authentication).getPrincipal();

		return createContextFor(type, authentication);
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, Authentication authentication) {
		final var claims = JwtClaimsSet.builder().subject("test-subject");

		return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
				.principal(authentication)
				.tokenType(type)
				.build();
	}

}
