package com.konfigyr.mcp.tool;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Iterator;

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
