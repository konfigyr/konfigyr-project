package com.konfigyr.identity.authorization.client;

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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@TestProfile
@SpringBootTest
@ImportTestcontainers(TestContainers.class)
class RegisteredNamespaceClientRepositoryTest extends AbstractClientRepositoryTest {

	@Autowired
	DSLContext context;

	@Autowired
	AuthorizationProperties properties;

	RegisteredNamespaceClientRepository repository;

	@BeforeEach
	void setup() {
		repository = new RegisteredNamespaceClientRepository(properties, context);
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
	@DisplayName("should retrieve client by client_id")
	void retrieveByClientId() {
		assertThat(repository.findByClientId("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp"))
				.isNotNull()
				.returns("Konfigyr active app", RegisteredClient::getClientName)
				.satisfies(assertClientId("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp", Duration.ofDays(7)))
				.satisfies(assertClientSecret("4b6dHEXXnAEMM1AD4b6RhqamjFwMdhIRgpyBVJRu-Zk", Duration.ofDays(3)))
				.satisfies(assertScopes("namespaces"))
				.satisfies(assertAuthorizationGrantTypes(
						AuthorizationGrantType.CLIENT_CREDENTIALS,
						AuthorizationGrantType.REFRESH_TOKEN
				))
				.satisfies(assertClientAuthenticationMethods())
				.satisfies(assertTokenSettings())
				.satisfies(assertClientSettings())
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

	@Test
	@DisplayName("should retrieve client by client registration identifier")
	void retrieveByRegistrationId() {
		assertThat(repository.findById("kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD"))
				.returns("Konfigyr expired app", RegisteredClient::getClientName)
				.satisfies(assertClientId("kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD", Duration.ofDays(30)))
				.satisfies(assertClientSecret("10S6cd0JgdO6WCLmOLB46d-Enx7K20hKSF1qicfev5g", Duration.ofDays(3).negated()))
				.satisfies(assertScopes("namespaces"))
				.satisfies(assertAuthorizationGrantTypes(
						AuthorizationGrantType.CLIENT_CREDENTIALS,
						AuthorizationGrantType.REFRESH_TOKEN
				))
				.satisfies(assertClientAuthenticationMethods())
				.satisfies(assertTokenSettings())
				.satisfies(assertClientSettings())
				.satisfies(assertRedirectUris())
				.satisfies(assertLogoutRedirectUris());
	}

}
