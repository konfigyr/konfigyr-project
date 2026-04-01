package com.konfigyr.data;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

@NullMarked
record DefaultCursorPage<T>(
		List<T> content,
		@Nullable CursorPageable nextPageable,
		@Nullable CursorPageable previousPageable
) implements CursorPage<T> {

	@Override
	public boolean hasNext() {
		return nextPageable != null;
	}

	@Override
	public boolean hasPrevious() {
		return previousPageable != null;
	}

	@Override
	public Iterator<T> iterator() {
		return content.iterator();
	}

	@Override
	public String toString() {
		return "CursorSlice(content=" + content + ", next=" + nextPageable + ", previous=" + previousPageable + ")";
	}
}
