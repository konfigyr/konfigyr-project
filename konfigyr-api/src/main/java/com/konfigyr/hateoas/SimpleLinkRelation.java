package com.konfigyr.hateoas;

import org.jspecify.annotations.NullMarked;

@NullMarked
record SimpleLinkRelation(String value) implements LinkRelation {

	@Override
	public String get() {
		return value;
	}

	@Override
	public String toString() {
		return "LinkRelation(" + value + ")";
	}
}
