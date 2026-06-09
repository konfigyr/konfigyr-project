package com.konfigyr.identity.authorization.client;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static com.konfigyr.identity.authorization.client.RegisteredNamespaceClientRepository.PIPELINE_ISSUER_URI;
import static com.konfigyr.identity.authorization.client.RegisteredNamespaceClientRepository.PIPELINE_SUBJECT_PATTERN;

@TestProfile
@SpringBootTest
@ImportTestcontainers(TestContainers.class)
class RegisteredNamespaceClientRepositoryTest extends AbstractClientRepositoryTest {

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
						"namespaces:publish-manifests",
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
				.satisfies(assertRedirectUris("http://localhost:56789/callback"))
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
						"namespaces:publish-manifests",
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
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

	@Test
	@DisplayName("should retrieve pipeline client by client registration identifier")
	void retrievePipelineClientByRegistrationId() {
		final var id = EntityId.from(6).serialize();

		assertThat(repository.findById(id))
				.returns("Konfigyr pipeline app", RegisteredClient::getClientName)
				.satisfies(assertClientId(id, "kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", Duration.ofHours(15)))
				.satisfies(assertClientSecret("iHZFaUowdtm2R9-7jOBuMucYj-E2jHlDPsaZlgUEUK4", Duration.ofDays(7)))
				.satisfies(assertScopes(
						"namespaces:read",
						"namespaces:publish-manifests"
				))
				.satisfies(assertAuthorizationGrantTypes(AuthorizationGrantType.TOKEN_EXCHANGE))
				.satisfies(assertClientAuthenticationMethods())
				.satisfies(it -> assertThat(it.getTokenSettings())
						.isNotNull()
						.returns(Duration.ofMinutes(30), TokenSettings::getAccessTokenTimeToLive)
						.returns(Duration.ofDays(1), TokenSettings::getRefreshTokenTimeToLive)
						.returns(false, TokenSettings::isReuseRefreshTokens)
				)
				.satisfies(assertClientSettings(false))
				.satisfies(it -> assertThat(it.getClientSettings())
						.returns("https://token.actions.githubusercontent.com", settings -> settings.getSetting(PIPELINE_ISSUER_URI))
						.returns("repo:konfigyr/*:ref:refs/heads/main", settings -> settings.getSetting(PIPELINE_SUBJECT_PATTERN))
				)
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

}
