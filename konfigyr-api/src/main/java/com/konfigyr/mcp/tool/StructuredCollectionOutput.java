package com.konfigyr.mcp.tool;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link StructuredOutput} wrapping a {@link Collection} of values.
 * <p>
 * Iterable over its {@code contents} for convenience.
 *
 * @param contents the wrapped collection, never {@literal null}
 * @param <T> the collection's element type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record StructuredCollectionOutput<T>(Collection<T> contents) implements StructuredOutput<Collection<T>>, Iterable<T> {

	@Override
	public Collection<T> get() {
		return contents;
	}

	@Override
	public Iterator<T> iterator() {
		return contents.iterator();
	}
}
