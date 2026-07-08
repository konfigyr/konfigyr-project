package com.konfigyr.identity.authorization.client;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AbstractIntegrationTest;
import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceClientType;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RegisteredNamespaceClientRepositoryTest extends AbstractIntegrationTest implements ClientRepositoryTestSupport {

	@Autowired
	DSLContext context;

	@Autowired
	AuthorizationProperties properties;

	@Autowired
	JsonMapper jsonMapper;

	RegisteredNamespaceClientRepository repository;

	@BeforeEach
	void setup() {
		repository = new RegisteredNamespaceClientRepository(properties, context, jsonMapper);
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
		assertThat(repository.findById("kfg-A2c7mvoxEP1rb"))
				.isNull();
	}

	@Test
	@DisplayName("should retrieve agent client by client_id")
	void retrieveByClientId() {
		assertThat(repository.findByClientId("kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY"))
				.isNotNull()
				.returns("Konfigyr agent app", RegisteredClient::getClientName)
				.satisfies(assertClientId(EntityId.from(5).serialize(), "kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY", Duration.ofDays(1)))
				.satisfies(assertNoClientSecret())
				.satisfies(assertScopes(
						"namespaces:read",
						"namespaces:delete",
						"namespaces:publish-releases",
						"namespaces:write",
						"namespaces:invite",
						"namespaces"
				))
				.satisfies(assertAuthorizationGrantTypes(AuthorizationGrantType.AUTHORIZATION_CODE))
				.satisfies(assertClientAuthenticationMethods(ClientAuthenticationMethod.NONE))
				.satisfies(it -> assertThat(it.getTokenSettings())
						.isNotNull()
						.returns(Duration.ofHours(1), TokenSettings::getAccessTokenTimeToLive)
						.returns(Duration.ofDays(7), TokenSettings::getRefreshTokenTimeToLive)
						.returns(false, TokenSettings::isReuseRefreshTokens)
				)
				.satisfies(assertClientSettings(true))
				.satisfies(assertClientSettings(2, NamespaceClientType.AGENT))
				.satisfies(assertRedirectUris("http://localhost/callback", "http://localhost:56789/callback"))
				.satisfies(assertLogoutRedirectUris());
	}

	@Test
	@DisplayName("should retrieve service account client by client registration identifier")
	void retrieveServiceAccountByRegistrationId() {
		final var id = EntityId.from(1).serialize();

		assertThat(repository.findById(id))
				.returns("Konfigyr expired app", RegisteredClient::getClientName)
				.satisfies(assertClientId(id, "kfg-AQEAAAAAAAAAAgAAAABqJToWfXkWbVML9iZbEPVai4o", Duration.ofDays(30)))
				.satisfies(assertClientSecret("10S6cd0JgdO6WCLmOLB46d-Enx7K20hKSF1qicfev5g", Duration.ofDays(3).negated()))
				.satisfies(assertScopes(
						"namespaces:read",
						"namespaces:delete",
						"namespaces:publish-releases",
						"namespaces:write",
						"namespaces:invite",
						"namespaces"
				))
				.satisfies(assertAuthorizationGrantTypes(AuthorizationGrantType.CLIENT_CREDENTIALS))
				.satisfies(assertClientAuthenticationMethods())
				.satisfies(it -> assertThat(it.getTokenSettings())
						.isNotNull()
						.returns(Duration.ofMinutes(20), TokenSettings::getAccessTokenTimeToLive)
						.returns(Duration.ofDays(7), TokenSettings::getRefreshTokenTimeToLive)
						.returns(false, TokenSettings::isReuseRefreshTokens)
				)
				.satisfies(assertClientSettings(false))
				.satisfies(assertClientSettings(2, NamespaceClientType.SERVICE_ACCOUNT))
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

	@Test
	@DisplayName("should retrieve workload client by client registration identifier")
	void retrieveWorkloadClientByRegistrationId() {
		final var id = EntityId.from(6).serialize();

		assertThat(repository.findById(id))
				.returns("Konfigyr workload app", RegisteredClient::getClientName)
				.satisfies(assertClientId(id, "kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", Duration.ofHours(15)))
				.satisfies(assertNoClientSecret())
				.satisfies(assertScopes(
						"namespaces:read",
						"namespaces:publish-releases"
				))
				.satisfies(assertAuthorizationGrantTypes(AuthorizationGrantType.TOKEN_EXCHANGE))
				.satisfies(assertClientAuthenticationMethods(ClientAuthenticationMethod.NONE))
				.satisfies(it -> assertThat(it.getTokenSettings())
						.isNotNull()
						.returns(Duration.ofMinutes(30), TokenSettings::getAccessTokenTimeToLive)
						.returns(Duration.ofDays(1), TokenSettings::getRefreshTokenTimeToLive)
						.returns(false, TokenSettings::isReuseRefreshTokens)
				)
				.satisfies(assertClientSettings(false))
				.satisfies(assertClientSettings(2, NamespaceClientType.WORKLOAD))
				.satisfies(it -> assertThat(it.getClientSettings())
						.returns("https://token.actions.githubusercontent.com",
								settings -> settings.getSetting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI))
						.returns("repo:konfigyr/*:ref:refs/heads/main",
								settings -> settings.getSetting(NamespaceClientSettingNames.WORKLOAD_SUBJECT_PATTERN))
				)
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

	static Consumer<RegisteredClient> assertClientSettings(long namespace, NamespaceClientType type) {
		return client -> assertThat(client.getClientSettings())
				.returns(EntityId.from(namespace), settings -> settings.getSetting(NamespaceClientSettingNames.NAMESPACE))
				.returns(type, settings -> settings.getSetting(NamespaceClientSettingNames.CLIENT_TYPE));
	}

}
