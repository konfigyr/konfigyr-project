package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;

import java.io.Serial;
import java.io.Serializable;

/**
 * Additional, per-type hints for {@link JsonSchemaGenerator#generate(ResolvableType, JsonSchemaGeneratorHints)}
 * that can't be derived from the Java type alone.
 * <p>
 * {@link #description()} is applied directly onto whatever {@link JsonSchema} builder gets
 * resolved for the type, regardless of which provider or fallback produced it.
 *
 * @param description a description of the type, can be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record JsonSchemaGeneratorHints(@Nullable String description) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6687614319128593127L;

	private static final JsonSchemaGeneratorHints NONE = new JsonSchemaGeneratorHints(null);

	/**
	 * Returns the shared hints instance carrying no description.
	 *
	 * @return the empty hints instance, never {@literal null}
	 */
	public static JsonSchemaGeneratorHints none() {
		return NONE;
	}

}
