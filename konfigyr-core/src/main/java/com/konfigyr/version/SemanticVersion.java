package com.konfigyr.version;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

record SemanticVersion(
		int major,
		int minor,
		int patch,
		MetadataVersion preRelease,
		MetadataVersion build,
		String original
) implements Version {

	@Serial
	private static final long serialVersionUID = 2009891062895544468L;

	SemanticVersion(int major, int minor, int patch, String original) {
		this(major, minor, patch, MetadataVersion.EMPTY, MetadataVersion.EMPTY, original);
	}

	@NonNull
	@Override
	public String get() {
		return original;
	}

	@Override
	public int compareTo(@NonNull Version o) {
		return o instanceof SemanticVersion other ? compare(other) : 0;
	}

	@NonNull
	@Override
	public String toString() {
		return "SemanticVersion(" + original + ")";
	}

	private int compare(@NonNull SemanticVersion other) {
		int result = Integer.compare(this.major, other.major);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(this.minor, other.minor);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(this.patch, other.patch);
		return result == 0 ? this.preRelease.compareTo(other.preRelease) : result;
	}
}
