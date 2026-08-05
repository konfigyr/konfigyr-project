package com.konfigyr.mcp.schema;

import com.konfigyr.artifactory.JsonSchema;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link JsonSchema.Builder} that wraps an already-built {@link JsonSchema}, returning it as-is
 * from {@link #build()} regardless of any further builder calls made on it.
 * <p>
 * Bridges {@link JsonSchemaDefinitionProvider#provide} back to {@link JsonSchema}, since there is
 * no way to turn an already-built schema back into a builder.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class PassthroughJsonSchemaBuilder extends JsonSchema.Builder<JsonSchema, PassthroughJsonSchemaBuilder> {

	private final JsonSchema schema;

	PassthroughJsonSchemaBuilder(JsonSchema schema) {
		super(schema.type());
		this.schema = schema;
	}

	@Override
	public JsonSchema build() {
		return schema;
	}

}
