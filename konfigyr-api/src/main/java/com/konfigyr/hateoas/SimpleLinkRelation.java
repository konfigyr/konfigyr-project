package com.konfigyr.hateoas;

import org.springframework.lang.NonNull;

record SimpleLinkRelation(String value) implements LinkRelation {

	@NonNull
	@Override
	public String get() {
		return value;
	}

	@Override
	public String toString() {
		return "LinkRelation(" + value + ")";
	}
}
