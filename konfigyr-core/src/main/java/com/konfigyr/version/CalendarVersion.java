package com.konfigyr.version;

import org.springframework.lang.NonNull;

import java.io.Serial;
import java.time.Year;

record CalendarVersion(
		Year year,
		int primary,
		int secondary,
		String modifier,
		String original
) implements Version {

	@Serial
	private static final long serialVersionUID = 5003471058787236961L;

	@NonNull
	@Override
	public String get() {
		return original;
	}

	/**
	 * Compares this CalVerVersion to another Version.
	 * Sorting order: year > primary > secondary > modifier (lexicographical).
	 * Missing modifiers are treated as greater than existing modifiers (similar to SemVer pre-release).
	 */
	@Override
	public int compareTo(@NonNull Version o) {
		return o instanceof CalendarVersion other ? compare(other) : 0;
	}

	@NonNull
	@Override
	public String toString() {
		return "CalendarVersion(" + original + ")";
	}

	private int compare(@NonNull CalendarVersion other) {
		int result = this.year.compareTo(other.year);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(this.primary, other.primary);
		if (result != 0) {
			return result;
		}

		result = Integer.compare(this.secondary, other.secondary);
		if (result != 0) {
			return result;
		}

		if (this.modifier == null && other.modifier == null) {
			return 0;
		}

		if (this.modifier == null) {
			return 1;
		}

		if (other.modifier == null) {
			return -1;
		}

		return this.modifier.compareTo(other.modifier);
	}

}
