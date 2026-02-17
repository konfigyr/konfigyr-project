package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Duration;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class KonfigyrRegisteredClientRepositoryTest extends AbstractClientRepositoryTest {

	AuthorizationProperties properties;
	KonfigyrRegisteredClientRepository repository;

	@BeforeEach
	void setup() {
		properties = new AuthorizationProperties();
		properties.setClientId("konfigyr-client");
		properties.setClientSecret("{noop}konfigyr-secret");
		properties.setRedirectUris(Set.of("http://localhost/callback", "https://konfigyr.com/callback"));
		properties.setPostLogoutRedirectUris(Set.of("http://localhost/logout-callback"));
		properties.getToken().setAccessTokenTimeToLive(Duration.ofMinutes(15));
		properties.getToken().setRefreshTokenTimeToLive(Duration.ofDays(7));

		repository = new KonfigyrRegisteredClientRepository(properties);
	}

	@Test
	@DisplayName("should throw operation not supported when trying to save client")
	void shouldNotStoreClients() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.save(Mockito.mock(RegisteredClient.class)))
				.withMessageContaining("Registering OAuth clients is not supported")
				.withNoCause();
	}

	@Test
	@DisplayName("should retrieve client by invalid client_id")
	void retrieveByInvalidClientId() {
		assertThat(repository.findByClientId("invalid-client-id"))
				.isNull();
	}

	@Test
	@DisplayName("should retrieve client by unknown registration identifier")
	void retrieveByInvalidRegistrationId() {
		assertThat(repository.findById("invalid-client-registration"))
				.isNull();
	}

	@Test
	@DisplayName("should retrieve client by configured client_id")
	void retrieveByClientId() {
		assertThat(repository.findByClientId(properties.getClientId()))
				.isNotNull()
				.satisfies(assertBuiltInClient());
	}

	@Test
	@DisplayName("should retrieve client by built-in registration identifier")
	void retrieveByRegistrationId() {
		assertThat(repository.findById("konfigyr"))
				.isNotNull()
				.satisfies(assertBuiltInClient());
	}

	static Consumer<RegisteredClient> assertBuiltInClient() {
		return client -> assertThat(client)
				.isNotNull()
				.returns("Konfigyr OAuth Client", RegisteredClient::getClientName)
				.satisfies(assertClientId("konfigyr", "konfigyr-client", null))
				.satisfies(assertClientSecret("konfigyr-secret", null))
				.satisfies(assertScopes(
						"openid",
						"namespaces",
						"namespaces:read",
						"namespaces:delete",
						"namespaces:write",
						"namespaces:invite",
						"profiles",
						"profiles:read",
						"profiles:write",
						"profiles:delete"
				))
				.satisfies(assertAuthorizationGrantTypes(
						AuthorizationGrantType.AUTHORIZATION_CODE,
						AuthorizationGrantType.CLIENT_CREDENTIALS,
						AuthorizationGrantType.REFRESH_TOKEN
				))
				.satisfies(assertClientAuthenticationMethods())
				.satisfies(assertTokenSettings())
				.satisfies(assertClientSettings())
				.satisfies(assertRedirectUris("http://localhost/callback", "https://konfigyr.com/callback"))
				.satisfies(assertLogoutRedirectUris("http://localhost/logout-callback"));
	}

}
