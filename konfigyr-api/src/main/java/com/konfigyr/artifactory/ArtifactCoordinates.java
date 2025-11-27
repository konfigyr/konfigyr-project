package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.konfigyr.version.Version;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

/**
 * Interface that describes the Maven Coordinates that artifacts use.
 * <p>
 * Coordinates consists out of the three integral fields: {@code groupId:artifactId:version}.
 * These three fields act much like an address and timestamp in one. This marks a specific place in
 * a repository, acting like a coordinate system for Maven projects.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface ArtifactCoordinates extends Comparable<ArtifactCoordinates>, Serializable {

	@JsonCreator
	static ArtifactCoordinates parse(String coordinates) {
		return SimpleArtifactCoordinates.parse(coordinates);
	}

	static ArtifactCoordinates of(String groupId, String artifactId, String version) {
		return new SimpleArtifactCoordinates(groupId, artifactId, version);
	}

	static ArtifactCoordinates of(String groupId, String artifactId, Version version) {
		return new SimpleArtifactCoordinates(groupId, artifactId, version);
	}

	/**
	 * Compares the two {@link ArtifactCoordinates} for order by comparing all three coordinate fields.
	 *
	 * @param a the first coordinates, can't be {@literal null}.
	 * @param b the second coordinates, can't be {@literal null}.
	 * @return a negative integer, zero, or a positive integer as the first coordinates are less than, equal to,
	 * or greater than the second.
	 */
	static int compare(@NonNull ArtifactCoordinates a, @NonNull ArtifactCoordinates b) {
		return SimpleArtifactCoordinates.COMPARATOR.compare(a, b);
	}

	/**
	 * Returns the {@code groupId} Maven coordinate of the artifact.
	 *
	 * @return the {@code groupId} Maven coordinate, can't be {@literal null}
	 */
	@NonNull
	String groupId();

	/**
	 * Returns the {@code artifactId} Maven coordinate of the artifact.
	 *
	 * @return the {@code artifactId} Maven coordinate, can't be {@literal null}
	 */
	@NonNull
	String artifactId();

	/**
	 * Returns the {@code version} Maven coordinate of the artifact.
	 *
	 * @return the {@code version} Maven coordinate, can't be {@literal null}
	 */
	@NonNull
	Version version();

	/**
	 * Formats the Artifact coordinates into a textual representation.
	 *
	 * @return the textual representation for the coordinates, can't be {@literal null}
	 */
	@NonNull
	default String format() {
		return String.format("%s:%s:%s", groupId(), artifactId(), version().get());
	}

	@Override
	default int compareTo(@NonNull ArtifactCoordinates o) {
		return compare(this, o);
	}
}
