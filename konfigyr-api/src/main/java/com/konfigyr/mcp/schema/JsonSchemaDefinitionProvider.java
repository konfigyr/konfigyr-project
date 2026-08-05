package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;

/**
 * Composable extension point for {@link DefaultJsonSchemaGenerator}: attempts to provide a
 * {@link JsonSchema} definition for a well-known type, without needing to know anything about
 * structural types (arrays, collections, maps) or POJOs.
 * <p>
 * Other packages can register their own provider for types this package has no business knowing
 * about, e.g. a tool-specific output wrapper, via {@link JsonSchemaGenerator#withProviders}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface JsonSchemaDefinitionProvider {

	/**
	 * Attempts to provide a {@link JsonSchema} builder for the given type.
	 * <p>
	 * The given {@link JsonSchemaGenerator} is the one driving this call, and may be used to
	 * recursively generate a schema for a type nested in or wrapped by the given type. Wrap the
	 * result of such a recursive call with {@link #passthrough(JsonSchema)} to satisfy this
	 * method's builder-returning contract.
	 *
	 * @param type the type for which a schema definition is requested, can't be {@literal null}
	 * @param generator the generator driving this call, can't be {@literal null}
	 * @return the resolved schema builder, or {@literal null} if this provider has no definition
	 * for the given type
	 */
	JsonSchema.@Nullable Builder<?, ?> provide(ResolvableType type, JsonSchemaGenerator generator);

	/**
	 * Wraps an already-built {@link JsonSchema} as a builder that returns it as-is from
	 * {@code build()}.
	 * <p>
	 * Intended for providers that resolve their schema by recursively calling a
	 * {@link JsonSchemaGenerator}, which only hands back built schemas, not builders.
	 *
	 * @param schema the already-built schema to wrap, can't be {@literal null}
	 * @return a builder wrapping the given schema, never {@literal null}
	 */
	static JsonSchema.Builder<?, ?> passthrough(JsonSchema schema) {
		return new PassthroughJsonSchemaBuilder(schema);
	}

}
