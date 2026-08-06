package com.konfigyr.mcp.tool;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.Nullable;

/**
 * A {@link StructuredOutput} wrapping a single, possibly {@literal null}, value.
 * <p>
 * Serializes as the wrapped value itself - see {@link #get()} - rather than as an object with a
 * {@code contents} property.
 *
 * @param contents the wrapped value, can be {@literal null}
 * @param <T> the wrapped value's type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record StructuredEntityOutput<T>(@Nullable T contents) implements StructuredOutput<T> {

	@Override
	@JsonValue
	public T get() {
		return contents;
	}

}
