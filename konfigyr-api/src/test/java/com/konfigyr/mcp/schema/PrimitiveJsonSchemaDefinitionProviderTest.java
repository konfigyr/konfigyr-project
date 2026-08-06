package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.BooleanSchema;
import com.konfigyr.artifactory.IntegerSchema;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.NumberSchema;
import com.konfigyr.artifactory.StringSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PrimitiveJsonSchemaDefinitionProviderTest {

	final PrimitiveJsonSchemaDefinitionProvider provider = new PrimitiveJsonSchemaDefinitionProvider();

	@Mock
	JsonSchemaGenerator generator;

	@Test
	@DisplayName("should provide a plain string schema for String, with no format")
	void shouldProvidePlainStringSchema() {
		assertThat(build(String.class))
				.isInstanceOfSatisfying(StringSchema.class, schema -> assertThat(schema.format()).isNull());
	}

	@ParameterizedTest
	@DisplayName("should provide a formatted string schema for well-known string-like types")
	@MethodSource("formattedStringTypes")
	void shouldProvideFormattedStringSchema(Class<?> type, String expectedFormat) {
		assertThat(build(type))
				.isInstanceOfSatisfying(StringSchema.class, schema -> assertThat(schema.format()).isEqualTo(expectedFormat));
	}

	@ParameterizedTest
	@DisplayName("should provide a formatted integer schema for byte/short/int/long and their wrappers")
	@MethodSource("integerTypes")
	void shouldProvideFormattedIntegerSchema(Class<?> type, String expectedFormat) {
		assertThat(build(type))
				.isInstanceOfSatisfying(IntegerSchema.class, schema -> assertThat(schema.format()).isEqualTo(expectedFormat));
	}

	@Test
	@DisplayName("should provide an integer schema with no format for BigInteger")
	void shouldProvideIntegerSchemaForBigInteger() {
		assertThat(build(BigInteger.class))
				.isInstanceOfSatisfying(IntegerSchema.class, schema -> assertThat(schema.format()).isNull());
	}

	@ParameterizedTest
	@DisplayName("should provide a formatted number schema for float/double and their wrappers")
	@MethodSource("numberTypes")
	void shouldProvideFormattedNumberSchema(Class<?> type, String expectedFormat) {
		assertThat(build(type))
				.isInstanceOfSatisfying(NumberSchema.class, schema -> assertThat(schema.format()).isEqualTo(expectedFormat));
	}

	@Test
	@DisplayName("should provide a number schema with no format for BigDecimal")
	void shouldProvideNumberSchemaForBigDecimal() {
		assertThat(build(BigDecimal.class))
				.isInstanceOfSatisfying(NumberSchema.class, schema -> assertThat(schema.format()).isNull());
	}

	@ParameterizedTest
	@DisplayName("should provide a boolean schema for boolean and Boolean")
	@MethodSource("booleanTypes")
	void shouldProvideBooleanSchema(Class<?> type) {
		assertThat(build(type)).isInstanceOf(BooleanSchema.class);
	}

	@Test
	@DisplayName("should fall back to a string schema for an unlisted CharSequence subtype")
	void shouldFallBackToStringSchemaForCharSequenceSubtype() {
		assertThat(build(StringBuilder.class)).isInstanceOf(StringSchema.class);
	}

	@Test
	@DisplayName("should fall back to a number schema for an unlisted Number subtype")
	void shouldFallBackToNumberSchemaForNumberSubtype() {
		assertThat(build(AtomicInteger.class)).isInstanceOf(NumberSchema.class);
	}

	@Test
	@DisplayName("should provide no schema for a type it doesn't recognize")
	void shouldProvideNoSchemaForUnknownType() {
		assertThat(provider.provide(ResolvableType.forClass(Object.class), generator)).isNull();
	}

	private JsonSchema build(Class<?> type) {
		final JsonSchema.Builder<?, ?> builder = provider.provide(ResolvableType.forClass(type), generator);

		assertThat(builder)
				.as("No schema provided for type %s", type)
				.isNotNull();

		verifyNoInteractions(generator);

		return builder.build();
	}

	static Stream<Arguments> formattedStringTypes() {
		return Stream.of(
				Arguments.of(UUID.class, "uuid"),
				Arguments.of(URI.class, "uri"),
				Arguments.of(Instant.class, "date-time"),
				Arguments.of(LocalDateTime.class, "date-time"),
				Arguments.of(OffsetDateTime.class, "date-time"),
				Arguments.of(ZonedDateTime.class, "date-time"),
				Arguments.of(LocalDate.class, "date"),
				Arguments.of(LocalTime.class, "time"),
				Arguments.of(Duration.class, "duration")
		);
	}

	static Stream<Arguments> integerTypes() {
		return Stream.of(
				Arguments.of(byte.class, "int8"),
				Arguments.of(Byte.class, "int8"),
				Arguments.of(short.class, "int16"),
				Arguments.of(Short.class, "int16"),
				Arguments.of(int.class, "int32"),
				Arguments.of(Integer.class, "int32"),
				Arguments.of(long.class, "int64"),
				Arguments.of(Long.class, "int64")
		);
	}

	static Stream<Arguments> numberTypes() {
		return Stream.of(
				Arguments.of(float.class, "float"),
				Arguments.of(Float.class, "float"),
				Arguments.of(double.class, "double"),
				Arguments.of(Double.class, "double")
		);
	}

	static Stream<Class<?>> booleanTypes() {
		return Stream.of(boolean.class, Boolean.class);
	}

}
