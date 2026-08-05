package com.konfigyr.mcp.tool;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.Nullable;

public record StructuredEntityOutput<T>(@Nullable T contents) implements StructuredOutput<T> {

	@Override
	@JsonValue
	public T get() {
		return contents;
	}

}
