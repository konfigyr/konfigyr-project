package com.konfigyr.version;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Interface that represents a sortable and immutable software version.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@ValueObject
public sealed interface Version extends Supplier<String>, Comparable<Version>, Serializable
	permits CalendarVersion, SemanticVersion, UnknownVersion {

	/**
	 * Attempts the parse the raw version string and return the appropriate {@link Version} instance.
	 *
	 * @param version the raw version string, can't be {@literal null}.
	 * @return the matching {@link Version} instance.
	 * @throws IllegalArgumentException when the given version string is blank or invalid
	 */
	@JsonCreator
	static Version of(String version) {
		return VersionParser.parse(version);
	}

	/**
	 * The method that returns the original string representation of the version.
	 *
	 * @return the original version, never {@literal null}.
	 */
	@NonNull
	@Override
	@JsonValue
	String get();

}
