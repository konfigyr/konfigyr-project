package com.konfigyr.mcp.tool;

import com.konfigyr.artifactory.ArraySchema;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.ObjectSchema;
import com.konfigyr.mcp.schema.JsonSchemaDefinitionProvider;
import com.konfigyr.mcp.schema.JsonSchemaGenerator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;

/**
 * Provides {@link JsonSchema} definitions for the {@link StructuredOutput} hierarchy.
 * <p>
 * A {@link StructuredEntityOutput} is transparent: its schema is the wrapped entity's own schema.
 * A {@link StructuredCollectionOutput} is wrapped in an object with a single required
 * {@code contents} array property, matching the structured content shape produced for it.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class StructuredOutputJsonSchemaDefinitionProvider implements JsonSchemaDefinitionProvider {

	@Override
	public JsonSchema.@Nullable Builder<?, ?> provide(ResolvableType type, JsonSchemaGenerator generator) {
		final Class<?> target = type.toClass();

		if (StructuredEntityOutput.class.isAssignableFrom(target)) {
			return JsonSchemaDefinitionProvider.passthrough(generator.generate(elementType(type)));
		}

		if (StructuredCollectionOutput.class.isAssignableFrom(target)) {
			return ObjectSchema.builder()
					.required("contents")
					.property("contents", ArraySchema.of(generator.generate(elementType(type))));
		}

		return null;
	}

	private static ResolvableType elementType(ResolvableType type) {
		final ResolvableType generic = type.getGeneric(0);
		return ResolvableType.NONE.equals(generic) ? ResolvableType.forClass(Object.class) : generic;
	}

}
