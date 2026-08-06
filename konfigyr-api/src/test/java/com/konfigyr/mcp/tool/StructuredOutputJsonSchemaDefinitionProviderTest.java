package com.konfigyr.mcp.tool;

import com.konfigyr.artifactory.ArraySchema;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.ObjectSchema;
import com.konfigyr.mcp.McpToolFixtures.Greeting;
import com.konfigyr.mcp.schema.JsonSchemaGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructuredOutputJsonSchemaDefinitionProviderTest {

	final StructuredOutputJsonSchemaDefinitionProvider provider = new StructuredOutputJsonSchemaDefinitionProvider();

	@Spy
	JsonSchemaGenerator generator = JsonSchemaGenerator.getInstance();

	@Test
	@DisplayName("should transparently return the wrapped entity's schema for a StructuredEntityOutput")
	void shouldUnwrapStructuredEntityOutput() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(StructuredEntityOutput.class, Greeting.class);
		final JsonSchema.Builder<?, ?> builder = provider.provide(type, generator);

		assertThat(builder).isNotNull();
		assertThat(builder.build())
				.isEqualTo(generator.generate(ResolvableType.forClass(Greeting.class)));
	}

	@Test
	@DisplayName("should default to Object for a raw StructuredEntityOutput with no type argument")
	void shouldDefaultToObjectElementTypeForRawStructuredEntityOutput() {
		final JsonSchema.Builder<?, ?> builder = provider.provide(ResolvableType.forClass(StructuredEntityOutput.class), generator);

		assertThat(builder).isNotNull();
		assertThat(builder.build())
				.isEqualTo(ObjectSchema.instance());
	}

	@Test
	@DisplayName("should wrap a StructuredCollectionOutput's element schema in a required contents array property")
	void shouldWrapStructuredCollectionOutput() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(StructuredCollectionOutput.class, Greeting.class);
		final JsonSchema.Builder<?, ?> builder = provider.provide(type, generator);

		assertThat(builder).isNotNull();
		assertThat(builder.build()).isInstanceOfSatisfying(ObjectSchema.class, schema -> {
			assertThat(schema.required()).containsExactly("contents");
			assertThat(schema.properties()).hasEntrySatisfying("contents", contents -> assertThat(contents)
					.isInstanceOfSatisfying(ArraySchema.class, array -> assertThat(array.items())
							.isEqualTo(generator.generate(ResolvableType.forClass(Greeting.class)))
					)
			);
		});
	}

	@Test
	@DisplayName("should provide no schema for a type it doesn't recognize")
	void shouldProvideNoSchemaForUnknownType() {
		assertThat(provider.provide(ResolvableType.forClass(String.class), generator)).isNull();
		verifyNoInteractions(generator);
	}

}
