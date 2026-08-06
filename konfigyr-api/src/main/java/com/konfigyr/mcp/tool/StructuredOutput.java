package com.konfigyr.mcp.tool;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Marker return type for {@code @}{@link com.konfigyr.mcp.annotation.McpTool} handler methods
 * that want a structured content representation alongside their text content, instead of
 * falling back to plain JSON serialization.
 * <p>
 * Sealed to {@link StructuredEntityOutput} and {@link StructuredCollectionOutput} - use the
 * {@code of(...)} factory methods rather than implementing this interface directly.
 * <p>
 * This is also the only return type an output schema is generated for during tool
 * specification/discovery: {@code McpToolSpecificationFactory} only generates a {@code Tool}'s
 * {@code outputSchema} when its handler method's return type is assignable to
 * {@link StructuredOutput}. A handler method returning any other type is still serialized fine
 * at invocation time, but the tool is advertised via {@code tools/list} with no output schema.
 *
 * @param <T> the wrapped value's type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public sealed interface StructuredOutput<T> extends Supplier<T> permits StructuredEntityOutput, StructuredCollectionOutput {

	/**
	 * Wraps a single, possibly {@literal null}, value as a {@link StructuredEntityOutput}.
	 *
	 * @param contents the value to wrap, can be {@literal null}
	 * @param <T> the wrapped value's type
	 * @return the structured entity output, never {@literal null}
	 */
	static <T> StructuredEntityOutput<@Nullable T> of(@Nullable T contents) {
		return new StructuredEntityOutput<>(contents);
	}

	/**
	 * Wraps a {@link Collection} as a {@link StructuredCollectionOutput}.
	 *
	 * @param collection the collection to wrap, treated as empty when {@literal null}
	 * @param <T> the collection's element type
	 * @return the structured collection output, never {@literal null}
	 */
	static <T> StructuredCollectionOutput<T> of(@Nullable Collection<T> collection) {
		return new StructuredCollectionOutput<>(collection == null ? Collections.emptyList() : collection);
	}

	/**
	 * Wraps a {@link Page}'s content as a {@link StructuredCollectionOutput}.
	 *
	 * @param page the page whose content is wrapped, can't be {@literal null}
	 * @param <T> the page's element type
	 * @return the structured collection output, never {@literal null}
	 */
	static <T> StructuredCollectionOutput<T> of(Page<T> page) {
		return of(page.getContent());
	}
}
