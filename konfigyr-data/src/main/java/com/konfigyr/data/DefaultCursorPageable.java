package com.konfigyr.data;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

record DefaultCursorPageable(@Nullable String token, int size) implements CursorPageable {

	DefaultCursorPageable {
		Assert.isTrue(size > 0, () -> "Cursor pageable size must be a positive number: " + size);
	}

	@NonNull
	@Override
	public String toString() {
		return "CursorPageable(token=" + token + ", size=" + size + ")";
	}

}
