package com.konfigyr.version;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

record UnknownVersion(String original) implements Version {

	@Serial
	private static final long serialVersionUID = 2363763362120851156L;

	@NonNull
	@Override
	public String get() {
		return original;
	}

	@Override
	public int compareTo(@NonNull Version o) {
		return o instanceof UnknownVersion other ? original.compareTo(other.original) : 0;
	}

	@NonNull
	@Override
	public String toString() {
		return "UnknownVersion(" + original + ")";
	}
}
