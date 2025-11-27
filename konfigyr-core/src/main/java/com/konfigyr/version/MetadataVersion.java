package com.konfigyr.version;

import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Record is used to represent the pre-release version and the build metadata.
 *
 * @param identifiers array containing the version's identifiers, never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
record MetadataVersion(String[] identifiers) implements Comparable<MetadataVersion>, Serializable {

	static final MetadataVersion EMPTY = new MetadataVersion(new String[0]);

	static MetadataVersion of(String identifiers) {
		return StringUtils.hasText(identifiers) ? new MetadataVersion(identifiers.split("\\.")) : EMPTY;
	}

	static MetadataVersion of(String... identifiers) {
		return identifiers == null || identifiers.length == 0 ? EMPTY : new MetadataVersion(identifiers);
	}

	@Serial
	private static final long serialVersionUID = 8668519943813447152L;

	@Override
	public int compareTo(@NonNull MetadataVersion other) {
		if (this == other) {
			return 0;
		}
		if (other == MetadataVersion.EMPTY) {
			return -1;
		}
		if (this == MetadataVersion.EMPTY) {
			return 1;
		}
		int result = compareIdentifierArrays(other.identifiers);
		if (result == 0) {
			// A larger set of pre-release fields has a higher precedence than a smaller set
			// if all the preceding identifiers are equal. (SemVer p.11)
			result = identifiers.length - other.identifiers.length;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof MetadataVersion version) {
			return Arrays.equals(this.identifiers, version.identifiers);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(identifiers);
	}

	@NonNull
	@Override
	public String toString() {
		return "MetadataVersion(" + Arrays.toString(identifiers) + ')';
	}

	private int compareIdentifierArrays(String[] other) {
		int result = 0;
		int length = Math.min(identifiers.length, other.length);
		for (int i = 0; i < length; i++) {
			result = compareIdentifiers(identifiers[i], other[i]);
			if (result != 0) {
				break;
			}
		}
		return result;
	}

	private int compareIdentifiers(String a, String b) {
		try {
			return Integer.parseInt(a) - Integer.parseInt(b);
		} catch (NumberFormatException ex) {
			// identifiers are not numbers, fallback to string compare
		}
		return a.compareTo(b);
	}
}
