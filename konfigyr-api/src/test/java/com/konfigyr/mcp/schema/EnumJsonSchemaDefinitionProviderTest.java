package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.StringSchema;
import com.konfigyr.vault.ChangeRequestState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnumJsonSchemaDefinitionProviderTest {

	final EnumJsonSchemaDefinitionProvider provider = new EnumJsonSchemaDefinitionProvider();

	@Mock
	JsonSchemaGenerator generator;

	@Test
	@DisplayName("should provide a string schema restricted to the enum's constant names")
	void shouldProvideEnumSchema() {
		final JsonSchema.Builder<?, ?> builder = provider.provide(ResolvableType.forClass(ChangeRequestState.class), generator);

		assertThat(builder)
				.isNotNull();

		assertThat(builder.build()).isInstanceOfSatisfying(StringSchema.class, schema ->
				assertThat(schema.enumerations())
						.containsExactlyInAnyOrder("DISCARDED", "MERGED", "OPEN")
		);

		verifyNoInteractions(generator);
	}

	@Test
	@DisplayName("should provide no schema for a non-enum type")
	void shouldProvideNoSchemaForNonEnum() {
		assertThat(provider.provide(ResolvableType.forClass(String.class), generator)).isNull();
		verifyNoInteractions(generator);
	}

}
