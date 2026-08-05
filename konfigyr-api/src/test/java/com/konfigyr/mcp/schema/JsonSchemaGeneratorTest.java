package com.konfigyr.mcp.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaGeneratorTest {

	final JsonSchemaGenerator generator = JsonSchemaGenerator.getInstance();

	@Test
	@DisplayName("should delegate to a definition provider for a primitive type")
	void shouldDelegateToProviderForPrimitiveType() {
		assertThat(generator.generate(ResolvableType.forClass(String.class)))
				.isEqualTo(StringSchema.instance());
	}

	@Test
	@DisplayName("should generate an array schema for a Java array")
	void shouldGenerateArraySchemaForArray() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(String[].class));

		assertThat(schema).isInstanceOfSatisfying(ArraySchema.class, array ->
				assertThat(array.items())
						.isEqualTo(StringSchema.instance())
		);
	}

	@Test
	@DisplayName("should generate an array schema with the element schema for a parameterized collection")
	void shouldGenerateArraySchemaForCollection() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(List.class, String.class);
		final JsonSchema schema = generator.generate(type, new JsonSchemaGeneratorHints("List of strings"));

		assertThat(schema).isInstanceOfSatisfying(ArraySchema.class, array -> {
			assertThat(array.description())
					.isEqualTo("List of strings");

			assertThat(array.items())
					.isEqualTo(StringSchema.instance());
		});
	}

	@Test
	@DisplayName("should default the element schema to a bare object for a raw collection")
	void shouldDefaultToObjectElementSchemaForRawCollection() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(List.class));

		assertThat(schema).isInstanceOfSatisfying(ArraySchema.class, array ->
				assertThat(array.items())
						.isInstanceOfSatisfying(ObjectSchema.class, object -> assertThat(object.properties()).isEmpty()));
	}

	@Test
	@DisplayName("should generate an object schema with additionalProperties for a map")
	void shouldGenerateObjectSchemaForMap() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, String.class, Integer.class);
		final JsonSchema schema = generator.generate(type);

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object ->
				assertThat(object.additionalProperties()).isInstanceOf(com.konfigyr.artifactory.IntegerSchema.class));
	}

	@Test
	@DisplayName("should unwrap an Optional to its element type's schema")
	void shouldUnwrapOptional() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(Optional.class, String.class);
		assertThat(generator.generate(type)).isInstanceOf(StringSchema.class);
	}

	@Test
	@DisplayName("should unwrap a Mono to its element type's schema")
	void shouldUnwrapMono() {
		final ResolvableType type = ResolvableType.forClassWithGenerics(Mono.class, String.class);
		assertThat(generator.generate(type)).isInstanceOf(StringSchema.class);
	}

	@Test
	@DisplayName("should generate an object schema for a record, marking primitive components as required")
	void shouldGenerateObjectSchemaForRecord() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Sample.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object -> {
			assertThat(object.properties()).containsOnlyKeys("name", "age");
			assertThat(object.properties().get("name")).isInstanceOf(StringSchema.class);
			assertThat(object.required()).containsExactly("age");
		});
	}

	@Test
	@DisplayName("should generate an object schema for a plain class the same way as for a record")
	void shouldGenerateObjectSchemaForPlainClass() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Plain.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object -> {
			assertThat(object.properties())
					.containsOnlyKeys("name", "age");

			assertThat(object.properties())
					.hasEntrySatisfying("name", it -> assertThat(it)
							.isEqualTo(StringSchema.instance())
					)
					.hasEntrySatisfying("age", it -> assertThat(it)
							.isEqualTo(IntegerSchema.builder()
									.format("int32")
									.build())
					);

			assertThat(object.required())
					.containsExactly("age");
		});
	}

	@Test
	@DisplayName("should rename a property using @JsonProperty")
	void shouldRenamePropertyUsingJsonProperty() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Renamed.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object ->
				assertThat(object.properties())
						.containsOnlyKeys("full_name")
		);
	}

	@Test
	@DisplayName("should exclude a property annotated with @JsonIgnore")
	void shouldExcludePropertyAnnotatedJsonIgnore() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Ignored.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object ->
				assertThat(object.properties())
						.containsOnlyKeys("visible")
		);
	}

	@Test
	@DisplayName("should set a property's description from @JsonPropertyDescription")
	void shouldSetPropertyDescriptionFromJsonPropertyDescription() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Described.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object ->
				assertThat(object.properties())
						.hasEntrySatisfying("name", it -> assertThat(it.description())
								.isEqualTo("the person's name")
				)
		);
	}

	@Test
	@DisplayName("should treat a @JsonValue-annotated type as its underlying value's type")
	void shouldTreatJsonValueAnnotatedTypeAsScalar() {
		assertThat(generator.generate(ResolvableType.forClass(EntityId.class)))
				.isInstanceOf(StringSchema.class);
	}

	@Test
	@DisplayName("should use hints when creating a JSON object schema")
	void shouldUseHintsWhenCreateSchema() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(Plain.class),
				new JsonSchemaGeneratorHints("Plain POJO"));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object -> {
			assertThat(object.description())
					.isEqualTo("Plain POJO");

			assertThat(object.properties())
					.containsOnlyKeys("name", "age");
		});
	}

	@Test
	@DisplayName("should not recurse forever on a self-referential type")
	void shouldNotRecurseForeverOnSelfReferentialType() {
		final JsonSchema schema = generator.generate(ResolvableType.forClass(SelfReferential.class));

		assertThat(schema).isInstanceOfSatisfying(ObjectSchema.class, object -> {
			assertThat(object.properties())
					.containsOnlyKeys("name", "parent");

			assertThat(object.properties().get("parent"))
					.isInstanceOfSatisfying(ObjectSchema.class, parent ->
							assertThat(parent.properties())
									.isEmpty()
					);
		});
	}

	@Test
	@DisplayName("should not recurse forever on mutually recursive types")
	void shouldNotRecurseForeverOnMutuallyRecursiveTypes() {
		assertThat(generator.generate(ResolvableType.forClass(NodeA.class)))
				.isInstanceOf(ObjectSchema.class);
	}

	record Sample(String name, int age) {

	}

	static class Plain {

		String name;
		int age;

	}

	record Renamed(@JsonProperty("full_name") String name) {

	}

	record Ignored(String visible, @JsonIgnore String hidden) {

	}

	record Described(@JsonPropertyDescription("the person's name") String name) {

	}

	record SelfReferential(String name, SelfReferential parent) {

	}

	record NodeA(String value, NodeB next) {

	}

	record NodeB(String value, NodeA next) {

	}

}
