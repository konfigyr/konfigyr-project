package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.ResolvableType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates a {@link JsonSchema} describing a given Java type.
 * <p>
 * Used both for a tool's input schema, generated from its handler method's parameters, and its
 * output schema, generated from the handler method's return type. The same generation rules
 * apply for both cases, except the tool's input schema's {@code required} list.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface JsonSchemaGenerator {

	/**
	 * Creates the default {@link JsonSchemaGenerator}, backed by every {@link JsonSchemaDefinitionProvider}
	 * this package ships with.
	 *
	 * @return the default generator, never {@literal null}
	 */
	static JsonSchemaGenerator getInstance() {
		return DefaultJsonSchemaGenerator.factory.get();
	}

	/**
	 * Creates a {@link JsonSchemaGenerator} backed by the default providers this package ships
	 * with, plus the given additional {@link JsonSchemaDefinitionProvider}s, tried before the
	 * defaults.
	 * <p>
	 * Lets other packages teach this generator about their own types, e.g. a tool-specific output
	 * wrapper, without the default {@link #getInstance()} singleton ever depending on them.
	 *
	 * @param providers additional providers to extend the default generator with, can't be {@literal null}
	 * @return the extended generator, never {@literal null}
	 */
	static JsonSchemaGenerator withProviders(JsonSchemaDefinitionProvider... providers) {
		final List<JsonSchemaDefinitionProvider> all = new ArrayList<>(Arrays.asList(providers));
		all.add(new PrimitiveJsonSchemaDefinitionProvider());
		all.add(new EnumJsonSchemaDefinitionProvider());
		return new DefaultJsonSchemaGenerator(all);
	}

	/**
	 * Generates a {@link JsonSchema} for the given type.
	 *
	 * @param type the type to generate a schema for, can't be {@literal null}
	 * @return the generated schema, never {@literal null}
	 */
	default JsonSchema generate(ResolvableType type) {
		return generate(type, JsonSchemaGeneratorHints.none());
	}

	/**
	 * Generates a {@link JsonSchema} for the given type and additional Schema generation hints.
	 *
	 * @param type the type to generate a schema for, can't be {@literal null}
	 * @param hints hints used when generating JSON schema, can't be {@literal null}
	 * @return the generated schema, never {@literal null}
	 */
	JsonSchema generate(ResolvableType type, JsonSchemaGeneratorHints hints);

}
