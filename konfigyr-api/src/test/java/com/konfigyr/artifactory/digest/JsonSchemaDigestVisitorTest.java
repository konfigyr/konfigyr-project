package com.konfigyr.artifactory.digest;

import com.konfigyr.artifactory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.MessageDigest;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class JsonSchemaDigestVisitorTest {

	JsonSchemaDigestVisitor visitor;

	@BeforeEach
	void setup() throws Exception {
		visitor = new JsonSchemaDigestVisitor(MessageDigest.getInstance("SHA-256"));
	}

	@Test
	@DisplayName("should generate unique digest for JSON schema describing a complex object")
	void generateSchemaForComplexObject() {
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

		assertThatNoException().isThrownBy(() -> visitor.visit(schema));

		assertThat(visitor.get())
				.asBase64Encoded()
				.isEqualTo("WJGIL6hYR9tW0E4kd+a+Q0PqFh+gkIDW8k90/SwikRs=");
	}

	@MethodSource("defaultSchemaInstances")
	@ParameterizedTest(name = "generate digest for: {0}")
	@DisplayName("should generate unique digests for default JSON schema instances")
	void generateDigestForJsonSchemaInstance(JsonSchema schema, String expected) {
		assertThatNoException().isThrownBy(() -> visitor.visit(schema));

		assertThat(visitor.get())
				.asBase64Encoded()
				.isEqualTo(expected);
	}

	static Stream<Arguments> defaultSchemaInstances() {
		return Stream.of(
				Arguments.of(BooleanSchema.instance(), "OnqI3txiywOTrr1ndIHMcvTQdV0bTL+Zv4Leo30lZ0U="),
				Arguments.of(NullSchema.instance(), "UHQ0b67r8KpqJsw3i4L1fUTUT5LI+KQj9HolWSPgLeA="),
				Arguments.of(StringSchema.instance(), "QFdOJTtHoUnlUiG9kYKx0h0ETPFsy6SoRvhBf8iOtWc="),
				Arguments.of(IntegerSchema.instance(), "XdHJtGkeaeAJ7MlyXFjfVjxMmgiCutN6r369iIff/ME="),
				Arguments.of(NumberSchema.instance(), "ITXd/grF1SJd3PDO1mErIBPq3hQD5Nngxr7rgXz/tDo="),
				Arguments.of(ArraySchema.instance(), "jUOW5c9T66nyaJen3uaotxOGFp1cHChOSd9RSufj76A="),
				Arguments.of(ObjectSchema.instance(), "+wp6+TnU7clC1BEwJZoQdXjXwvPOU5MscWslg28pHdA=")
		);
	}

}
