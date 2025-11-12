package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class NamespaceApplicationCredentialsTest {

	@Test
	@DisplayName("should generate unique client_id for OAuth namespace application")
	void shouldGenerateClientId() {
		final var definition = mock(NamespaceApplicationDefinition.class);
		doReturn(EntityId.from(245230241523503357L)).when(definition).namespace();

		assertThat(NamespaceApplicationDefinition.generateClientId(definition))
				.isNotBlank()
				.startsWith(NamespaceApplicationDefinition.CLIENT_ID_PREFIX)
				.hasSize(36)
				.isNotEqualTo(NamespaceApplicationDefinition.generateClientId(definition));
	}

	@Test
	@DisplayName("should generate unique client_secret for OAuth client_id")
	void shouldGenerateClientSecret() {
		final var clientId = "kfg-AAAAAAAAAALuyV9Zskx9qejO5PkQgzqa";

		assertThat(NamespaceApplicationDefinition.generateClientSecret(clientId))
				.isNotBlank()
				.hasSize(43)
				.isNotEqualTo(NamespaceApplicationDefinition.generateClientSecret(clientId));
	}

	@Test
	@DisplayName("should fail to generate unique client_secret for invalid client_id")
	void shouldFailToGenerateClientSecretForInvalidClientId() {
		assertThatIllegalStateException()
				.isThrownBy(() -> NamespaceApplicationDefinition.generateClientSecret("client_id"))
				.withMessageContaining("The OAuth client_id is invalid");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceApplicationDefinition.generateClientSecret(
						NamespaceApplicationDefinition.CLIENT_ID_PREFIX + "YWJjZA==="
				)).withMessageContaining("bad base-64");
	}

	@Test
	@DisplayName("should fail to generate unique client_secret for empty client_id")
	void shouldFailToGenerateClientSecretForEmptyClientId() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceApplicationDefinition.generateClientSecret(""))
				.withMessageContaining("The OAuth client_id can not be blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> NamespaceApplicationDefinition.generateClientSecret("   "))
				.withMessageContaining("The OAuth client_id can not be blank")
				.withNoCause();
	}


}
