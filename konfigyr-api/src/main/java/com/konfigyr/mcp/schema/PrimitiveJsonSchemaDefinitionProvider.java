package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.BooleanSchema;
import com.konfigyr.artifactory.IntegerSchema;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.NumberSchema;
import com.konfigyr.artifactory.StringSchema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Provides {@link JsonSchema} definitions for a fixed set of well-known JDK scalar types:
 * strings, numbers, booleans, UUIDs, URIs, and the most common {@code java.time} types.
 * <p>
 * Anything not covered by this fixed set is left to the next provider in the chain, or ultimately
 * to {@link DefaultJsonSchemaGenerator}'s structural/POJO fallback. More types can be registered
 * here as they come up; this isn't meant to be exhaustive from the start.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class PrimitiveJsonSchemaDefinitionProvider implements JsonSchemaDefinitionProvider {

	private final Map<Class<?>, Supplier<JsonSchema.Builder<?, ?>>> definitions = createDefinitions();

	@Override
	public JsonSchema.@Nullable Builder<?, ?> provide(ResolvableType type, JsonSchemaGenerator generator) {
		final Class<?> target = type.toClass();
		final Supplier<JsonSchema.Builder<?, ?>> definition = definitions.get(target);

		if (definition != null) {
			return definition.get();
		}

		if (CharSequence.class.isAssignableFrom(target)) {
			return StringSchema.builder();
		}

		if (Boolean.class.isAssignableFrom(target)) {
			return BooleanSchema.builder();
		}

		if (Number.class.isAssignableFrom(target)) {
			return NumberSchema.builder();
		}

		return null;
	}

	private static Map<Class<?>, Supplier<JsonSchema.Builder<?, ?>>> createDefinitions() {
		final Map<Class<?>, Supplier<JsonSchema.Builder<?, ?>>> definitions = new LinkedHashMap<>();

		definitions.put(String.class, StringSchema::builder);
		definitions.put(Character.class, StringSchema::builder);
		definitions.put(char.class, StringSchema::builder);
		definitions.put(UUID.class, () -> StringSchema.builder().format("uuid"));
		definitions.put(URI.class, () -> StringSchema.builder().format("uri"));

		definitions.put(Instant.class, () -> StringSchema.builder().format("date-time"));
		definitions.put(LocalDateTime.class, () -> StringSchema.builder().format("date-time"));
		definitions.put(OffsetDateTime.class, () -> StringSchema.builder().format("date-time"));
		definitions.put(ZonedDateTime.class, () -> StringSchema.builder().format("date-time"));
		definitions.put(LocalDate.class, () -> StringSchema.builder().format("date"));
		definitions.put(LocalTime.class, () -> StringSchema.builder().format("time"));
		definitions.put(Duration.class, () -> StringSchema.builder().format("duration"));

		definitions.put(Boolean.class, BooleanSchema::builder);
		definitions.put(boolean.class, BooleanSchema::builder);

		definitions.put(Byte.class, () -> IntegerSchema.builder().format("int8"));
		definitions.put(byte.class, () -> IntegerSchema.builder().format("int8"));
		definitions.put(Short.class, () -> IntegerSchema.builder().format("int16"));
		definitions.put(short.class, () -> IntegerSchema.builder().format("int16"));
		definitions.put(Integer.class, () -> IntegerSchema.builder().format("int32"));
		definitions.put(int.class, () -> IntegerSchema.builder().format("int32"));
		definitions.put(Long.class, () -> IntegerSchema.builder().format("int64"));
		definitions.put(long.class, () -> IntegerSchema.builder().format("int64"));
		definitions.put(BigInteger.class, IntegerSchema::builder);

		definitions.put(Float.class, () -> NumberSchema.builder().format("float"));
		definitions.put(float.class, () -> NumberSchema.builder().format("float"));
		definitions.put(Double.class, () -> NumberSchema.builder().format("double"));
		definitions.put(double.class, () -> NumberSchema.builder().format("double"));
		definitions.put(BigDecimal.class, NumberSchema::builder);

		return definitions;
	}

}
