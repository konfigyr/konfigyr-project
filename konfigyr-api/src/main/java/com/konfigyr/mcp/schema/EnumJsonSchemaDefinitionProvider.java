package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.StringSchema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;

/**
 * Provides a {@link StringSchema} definition for enum types, restricted to the enum's constant
 * names via {@link StringSchema.Builder#enumeration(String)}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class EnumJsonSchemaDefinitionProvider implements JsonSchemaDefinitionProvider {

	@Override
	public JsonSchema.@Nullable Builder<?, ?> provide(ResolvableType type, JsonSchemaGenerator generator) {
		final Class<?> target = type.toClass();

		if (!target.isEnum()) {
			return null;
		}

		final StringSchema.Builder schema = StringSchema.builder();

		for (Object constant : target.getEnumConstants()) {
			schema.enumeration(((Enum<?>) constant).name());
		}

		return schema;
	}

}
