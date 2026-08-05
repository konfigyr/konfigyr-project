package com.konfigyr.mcp.tool;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

@NullMarked
public sealed interface StructuredOutput<T> extends Supplier<T> permits StructuredEntityOutput, StructuredCollectionOutput {

	static <T> StructuredEntityOutput<@Nullable T> of(@Nullable T contents) {
		return new StructuredEntityOutput<>(contents);
	}

	static <T> StructuredCollectionOutput<T> of(@Nullable Collection<T> collection) {
		return new StructuredCollectionOutput<>(collection == null ? Collections.emptyList() : collection);
	}

	static <T> StructuredCollectionOutput<T> of(Page<T> page) {
		return of(page.getContent());
	}
}
