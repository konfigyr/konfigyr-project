package com.konfigyr.artifactory;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

class ArtifactoryJacksonModuleTest {

	final JsonMapper mapper = JsonMapper.builder()
			.addModule(new ArtifactoryJacksonModule())
			.build();

	@Test
	@DisplayName("should serialize and deserialize JSON schema objects")
	void serializeAndDeserializeJsonSchema() {
		final var schema = ObjectSchema.builder()
				.title("Test object")
				.description("Test object description")
				.required("username", "age", "active", "status")
				.additionalProperties(StringSchema.instance())
				.propertyNames(StringSchema.instance())
				.property("username", StringSchema.builder()
						.format("email")
						.minLength(12)
						.maxLength(255)
						.example("john.doe@konfigyr.com")
						.example("jane.doe@konfigyr.com")
						.build()
				)
				.property("active", BooleanSchema.builder()
						.description("Is the user active?")
						.defaultValue(true)
						.build()
				)
				.property("age", IntegerSchema.builder()
						.minimum(18L)
						.format("int32")
						.build()
				)
				.property("roles", ArraySchema.builder()
						.items(ObjectSchema.builder()
								.property("name", StringSchema.builder()
										.example("ADMIN")
										.example("USER")
										.example("VISITOR")
										.build()
								)
								.build()
						)
						.build()
				)
				.property("status", StringSchema.builder()
						.enumeration("ACTIVE")
						.enumeration("DISABLED")
						.enumeration("INACTIVE")
						.build()
				)
				.property("height", NumberSchema.builder()
						.minimum(160.00)
						.multipleOf(10.0)
						.deprecated(true)
						.build()
				)
				.property("createdAt", StringSchema.builder()
						.format("date-time")
						.build()
				)
				.property("expiresIn", StringSchema.builder()
						.format("duration")
						.build()
				)
				.build();

		final var json = mapper.writeValueAsString(schema);

		assertThat(json)
				.as("Serialized JSON schema should not be empty")
				.isNotBlank();

		assertThat(mapper.readValue(json, JsonSchema.class))
				.as("Deserialized JSON schema should be equal to the original")
				.isEqualTo(schema);
	}

	@Test
	@DisplayName("should serialize and deserialize default artifact metadata implementation")
	void serializeAndDeserializeMetadata() {
		final var metadata = TestArtifacts.metadata();
		final var json = mapper.writeValueAsString(metadata);

		assertThat(json)
				.as("Serialized Artifact metadata should not be empty")
				.isNotBlank();

		assertThatObject(mapper.readValue(json, ArtifactMetadata.class))
				.as("Deserialized Artifact metadata should be equal to the original")
				.isEqualTo(metadata);
	}

	@Test
	@DisplayName("should deserialize invalid artifact metadata payload")
	void deserializeInvalidMetadata() {
		final var json = mapper.getNodeFactory().objectNode()
				.put("name", "Konfigyr API")
				.set("properties", mapper.getNodeFactory().arrayNode()
						.add(mapper.getNodeFactory().objectNode())
				).toPrettyString();

		assertThatObject(mapper.readValue(json, ArtifactMetadata.class))
				.as("Deserialized Artifact metadata should be equal to the original")
				.isNotNull()
				.returns(null, ArtifactMetadata::groupId)
				.returns(null, ArtifactMetadata::artifactId)
				.returns(null, ArtifactMetadata::version)
				.returns("Konfigyr API", ArtifactMetadata::name)
				.extracting(ArtifactMetadata::properties, InstanceOfAssertFactories.iterable(PropertyDescriptor.class))
				.hasSize(1)
				.first()
				.returns(null, PropertyDescriptor::name)
				.returns(null, PropertyDescriptor::typeName)
				.returns(null, PropertyDescriptor::schema);
	}
}
