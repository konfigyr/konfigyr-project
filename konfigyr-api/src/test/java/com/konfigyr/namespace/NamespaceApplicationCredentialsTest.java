package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import com.konfigyr.security.OAuthScopes;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class NamespaceApplicationCredentialsTest {

	@Test
	@DisplayName("should generate unique client_id for OAuth namespace application")
	void shouldGenerateClientId() {
		final var definition = new NamespaceApplicationDefinition(
				EntityId.from(245230241523503357L),
				NamespaceClientType.SERVICE_ACCOUNT,
				"Test application",
				OAuthScopes.empty(),
				null,
				null
		);

		assertThat(NamespaceApplicationDefinition.generateClientId(definition))
				.isNotEqualTo(NamespaceApplicationDefinition.generateClientId(definition))
				.returns(definition.namespace(), NamespaceClientId::namespace)
				.returns(definition.type(), NamespaceClientId::type)
				.extracting(NamespaceClientId::timestamp, InstanceOfAssertFactories.INSTANT)
				.isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	@DisplayName("should generate unique client_secret for OAuth client_id using Service Account application type")
	void shouldGenerateClientSecret() {
		final var clientId = NamespaceClientId.parse("kfg-AQEDZzua-jEQ_QAAAABqJTeD8FHAZshfyP9Grh6WY28");

		assertThat(NamespaceApplicationDefinition.generateClientSecret(clientId))
				.isNotBlank()
				.hasSize(43)
				.isNotEqualTo(NamespaceApplicationDefinition.generateClientSecret(clientId));
	}

	@Test
	@DisplayName("should generate unique client_secret for OAuth client_id using Pipeline application type")
	void shouldGeneratePipelineSecret() {
		final var clientId = NamespaceClientId.parse("kfg-AQMDZzua-jEQ_QAAAABqJTdzkqoWr2y8UHktoN5S5IY");

		assertThat(NamespaceApplicationDefinition.generateClientSecret(clientId))
				.isNotBlank()
				.hasSize(43)
				.isNotEqualTo(NamespaceApplicationDefinition.generateClientSecret(clientId));
	}

	@Test
	@DisplayName("should fail to generate unique client_secret for Agent application type")
	void shouldNotGenerateClientSecretForAgentApplicationType() {
		final var clientId = NamespaceClientId.parse("kfg-AQIDZzua-jEQ_QAAAABqJTMymAV3HO6fuNJDclPfN_Q");

		assertThatExceptionOfType(NamespaceApplicationTypeException.class)
				.isThrownBy(() -> NamespaceApplicationDefinition.generateClientSecret(clientId))
				.withMessageContaining("Cannot reset client secret: %s applications do not use client secrets", clientId.type())
				.returns(clientId.type(), NamespaceApplicationTypeException::getClientType)
				.returns(NamespaceApplicationTypeException.ErrorCode.SECRET_NOT_SUPPORTED, NamespaceApplicationTypeException::getErrorCode);
	}

}
