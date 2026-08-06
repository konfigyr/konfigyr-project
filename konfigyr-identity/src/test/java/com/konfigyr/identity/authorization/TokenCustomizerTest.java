package com.konfigyr.identity.authorization;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.identity.authentication.OAuthAccountIdentityUser;
import com.konfigyr.security.NamespaceClientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimNames;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TokenCustomizerTest {

	final AccountIdentity identity = AccountIdentities.john().build();

	final TokenCustomizer customizer = new TokenCustomizer(List.of("konfigyr-api", "konfigyr-identity"));

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
				.hasSize(2)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.doesNotContainKey(KonfigyrClaimNames.NAMESPACE);
	}

	@Test
	@DisplayName("should throw when the JWT encoding context has no registered client attached")
	void throwsWhenRegisteredClientMissing() {
		final var authentication = mock(OAuth2ClientAuthenticationToken.class);
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, null, authentication);

		// Every real grant provider in this codebase (authorization_code, client_credentials,
		// refresh_token, token exchange) attaches a registered client to the token context before
		// customizers run - a context missing one is not a state `customize()` needs to tolerate.
		assertThatIllegalArgumentException()
				.isThrownBy(() -> customizer.customize(context))
				.withMessage("registeredClient cannot be null");
	}

	@Test
	@DisplayName("should not add namespace claim when the registered client id is not a namespace client id")
	void customizeAccessTokenForClient() {
		final var authentication = mock(OAuth2ClientAuthenticationToken.class);
		final var client = registeredClient("konfigyr")
				.clientName("Test client name")
				.build();

		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, client, authentication);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(3)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.NAME, "Test client name")
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.doesNotContainKey(KonfigyrClaimNames.NAMESPACE);
	}

	@Test
	@DisplayName("should add namespace claim when registered client is a namespace application")
	void customizeAccessTokenForNamespaceClient() {
		final var namespaceId = EntityId.from(42L);
		final var clientId = NamespaceClientId.of(namespaceId, NamespaceClientType.SERVICE_ACCOUNT);

		final var client = registeredClient(clientId)
				.clientName("Namespace app")
				.build();

		final var authentication = mock(OAuth2ClientAuthenticationToken.class);
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, client, authentication);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(4)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.NAME, "Namespace app")
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.containsEntry(KonfigyrClaimNames.NAMESPACE, namespaceId.serialize());
	}

	@Test
	@DisplayName("should not include personal information in OAuth Access token when no scopes are authorized")
	void customizeAccessTokenForIdentityWithoutScopes() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, identity);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(2)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.doesNotContainKey(StandardClaimNames.NAME)
				.doesNotContainKey(StandardClaimNames.EMAIL);
	}

	@Test
	@DisplayName("should customize OAuth Access token when account identity is present in Authentication")
	void customizeAccessTokenForIdentity() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, identity, OidcScopes.OPENID);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(4)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName())
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"));
	}

	@Test
	@DisplayName("should only include email in OAuth Access token when only email scope is authorized")
	void customizeAccessTokenForIdentityWithEmailScope() {
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, identity, OidcScopes.EMAIL);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(3)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.doesNotContainKey(StandardClaimNames.NAME);
	}

	@Test
	@DisplayName("should customize OAuth Access token when account identity user is present in Authentication")
	void customizeAccessTokenForUser() {
		final var user = new OAuthAccountIdentityUser(identity, mock(OAuth2User.class));
		final var client = registeredClient(NamespaceClientId.of(EntityId.from(1412465L), NamespaceClientType.WORKLOAD)).build();
		final var context = createContextFor(OAuth2TokenType.ACCESS_TOKEN, client, user, OidcScopes.OPENID);

		assertThatNoException().isThrownBy(() -> customizer.customize(context));

		assertThat(context.getClaims().build().getClaims())
				.hasSize(5)
				.containsEntry(StandardClaimNames.SUB, "test-subject")
				.containsEntry(StandardClaimNames.EMAIL, identity.getEmail())
				.containsEntry(StandardClaimNames.NAME, identity.getDisplayName())
				.containsEntry(OAuth2TokenClaimNames.AUD, List.of("konfigyr-api", "konfigyr-identity"))
				.containsEntry(KonfigyrClaimNames.NAMESPACE, EntityId.from(1412465L).serialize());
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

	static JwtEncodingContext createContextFor(OAuth2TokenType type, Object principal, String... scopes) {
		final var authentication = mock(Authentication.class);
		doReturn(principal).when(authentication).getPrincipal();

		return createContextFor(type, authentication, scopes);
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, Authentication authentication, String... scopes) {
		return createContextFor(type, registeredClient("test-client-id").build(), authentication, scopes);
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, RegisteredClient client, Object principal, String... scopes) {
		final var authentication = mock(Authentication.class);
		doReturn(principal).when(authentication).getPrincipal();

		return createContextFor(type, client, authentication, scopes);
	}

	static JwtEncodingContext createContextFor(OAuth2TokenType type, RegisteredClient client, Authentication authentication, String... scopes) {
		final var claims = JwtClaimsSet.builder().subject("test-subject");

		final var builder = JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claims)
				.authorizedScopes(Set.of(scopes))
				.principal(authentication)
				.tokenType(type);

		if (client != null) {
			builder.registeredClient(client);
		}

		return builder.build();
	}

	static RegisteredClient.Builder registeredClient(String clientId) {
		return RegisteredClient.withId("test-client-registration-id")
				.clientId(clientId)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
	}

	static RegisteredClient.Builder registeredClient(NamespaceClientId clientId) {
		return registeredClient(clientId.get())
				.clientSettings(ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, clientId.namespace())
						.build());
	}

}
