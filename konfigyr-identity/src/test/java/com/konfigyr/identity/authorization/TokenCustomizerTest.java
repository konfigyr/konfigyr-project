package com.konfigyr.identity.authorization;

import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.OAuthAccountIdentityUser;
import com.konfigyr.identity.authorization.jwk.KeyAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TokenCustomizerTest {

	final AccountIdentity identity = AccountIdentities.john().build();

	final TokenCustomizer customizer = new TokenCustomizer();

	@Test
	@DisplayName("should not customize access token")
	void ignoreAccessTokens() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, identity);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(1)
				.containsEntry("sub", "test-subject");
	}

	@Test
	@DisplayName("should not customize ID token when account identity is not present in Authentication")
	void ignoreInvalidAuthenticationTypes() {
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, "some principal");

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(1)
				.containsEntry("sub", "test-subject");
	}

	@Test
	@DisplayName("should customize ID token when account identity is present in Authentication")
	void customizeTokenForIdentity() {
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, identity);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(5)
				.containsEntry("sub", "test-subject")
				.containsEntry("oid", identity.getUsername())
				.containsEntry("email", identity.getEmail())
				.containsEntry("name", identity.getDisplayName())
				.containsEntry("picture", identity.getAvatar().get());
	}

	@Test
	@DisplayName("should customize ID token when account identity user is present in Authentication")
	void customizeTokenForUser() {
		final var user = new OAuthAccountIdentityUser(identity, mock(OAuth2User.class));
		final var context = createContextFor(TokenCustomizer.ID_TOKEN_TOKEN_TYPE, user);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(5)
				.containsEntry("sub", "test-subject")
				.containsEntry("oid", identity.getUsername())
				.containsEntry("email", identity.getEmail())
				.containsEntry("name", identity.getDisplayName())
				.containsEntry("picture", identity.getAvatar().get());
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, Object principal) {
		final var claims = JwtClaimsSet.builder().subject("test-subject");
		final var authentication = mock(Authentication.class);
		doReturn(principal).when(authentication).getPrincipal();

		return JwtEncodingContext.with(JwsHeader.with(KeyAlgorithm.RS256), claims)
				.principal(authentication)
				.tokenType(type)
				.build();
	}

}
