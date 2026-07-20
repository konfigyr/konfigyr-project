package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.version.Version;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface that describes the Maven Coordinates that artifacts use.
 * <p>
 * Coordinates consists out of the three integral fields: {@code groupId:artifactId:version}.
 * These three fields act much like an address and timestamp in one. This marks a specific place in
 * a repository, acting like a coordinate system for Maven projects.
 * <p>
 * The {@code groupId:artifactId} pair, without the version, is described by the parent {@link ArtifactKey}
 * interface.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ArtifactKey
 */
@NullMarked
public interface ArtifactCoordinates extends ArtifactKey, Comparable<ArtifactCoordinates> {

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of artifacts
	 * or property descriptors by their {@link ArtifactCoordinates groupId:artifactId:version} coordinates.
	 */
	SearchQuery.Criteria<ArtifactCoordinates> CRITERIA =
			SearchQuery.criteria("artifact.coordinates", ArtifactCoordinates.class);

	/**
	 * Parses the given textual representation of Maven coordinates and creates an
	 * {@link ArtifactCoordinates} instance.
	 * <p>
	 * The expected format is {@code groupId:artifactId:version}.
	 *
	 * @param coordinates the textual representation of the coordinates to parse, can be {@literal null}
	 * @return the parsed artifact coordinates, never {@literal null}
	 * @throws IllegalArgumentException if the coordinates string is invalid or cannot be parsed
	 */
	@JsonCreator
	static ArtifactCoordinates parse(@Nullable String coordinates) {
		return SimpleArtifactCoordinates.parse(coordinates);
	}

	/**
	 * Creates an {@link ArtifactCoordinates} instance from the given {@link Artifact}.
	 *
	 * @param artifact the artifact from which to extract coordinates, can't be {@literal null}
	 * @return the artifact coordinates, never {@literal null}
	 */
	static ArtifactCoordinates of(Artifact artifact) {
		return new SimpleArtifactCoordinates(artifact.groupId(), artifact.artifactId(), artifact.version());
	}

	/**
	 * Creates an {@link ArtifactCoordinates} instance from the given {@link ArtifactKey} and {@code version}.
	 *
	 * @param key the artifact key used to build the coordinates, can't be {@literal null}
	 * @param version the version of the artifact, can't be {@literal null}
	 * @return the artifact coordinates, never {@literal null}
	 */
	static ArtifactCoordinates of(ArtifactKey key, String version) {
		return new SimpleArtifactCoordinates(key.groupId(), key.artifactId(), version);
	}

	/**
	 * Creates an {@link ArtifactCoordinates} instance from the given Maven coordinate components.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @param version    the version as a string, can't be {@literal null}
	 * @return the artifact coordinates, never {@literal null}
	 */
	static ArtifactCoordinates of(String groupId, String artifactId, String version) {
		return new SimpleArtifactCoordinates(groupId, artifactId, version);
	}

	/**
	 * Creates an {@link ArtifactCoordinates} instance from the given Maven coordinate components.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @param version    the version object, can't be {@literal null}
	 * @return the artifact coordinates, never {@literal null}
	 */
	static ArtifactCoordinates of(String groupId, String artifactId, Version version) {
		return new SimpleArtifactCoordinates(groupId, artifactId, version);
	}

	/**
	 * Formats the given {@link Artifact} into a textual representation of Maven coordinates.
	 *
	 * @param artifact the artifact to format, can't be {@literal null}
	 * @return the formatted coordinates string in the format {@code groupId:artifactId:version}, never {@literal null}
	 */
	static String format(Artifact artifact) {
		return format(artifact.groupId(), artifact.artifactId(), artifact.version());
	}

	/**
	 * Formats the given Maven coordinate components into a textual representation.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @param version    the version as a string, can't be {@literal null}
	 * @return the formatted coordinates string in the format {@code groupId:artifactId:version}, never {@literal null}
	 */
	static String format(String groupId, String artifactId, String version) {
		return String.format("%s:%s:%s", groupId, artifactId, version);
	}

	/**
	 * Formats the given Maven coordinate components into a textual representation.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @param version    the version object, can't be {@literal null}
	 * @return the formatted coordinates string in the format {@code groupId:artifactId:version}, never {@literal null}
	 */
	static String format(String groupId, String artifactId, Version version) {
		return format(groupId, artifactId, version.get());
	}

	/**
	 * Compares the two {@link ArtifactCoordinates} for order by comparing all three coordinate fields.
	 *
	 * @param a the first coordinates, can't be {@literal null}.
	 * @param b the second coordinates, can't be {@literal null}.
	 * @return a negative integer, zero, or a positive integer as the first coordinates are less than, equal to,
	 * or greater than the second.
	 */
	static int compare(ArtifactCoordinates a, ArtifactCoordinates b) {
		return SimpleArtifactCoordinates.COMPARATOR.compare(a, b);
	}

	/**
	 * Returns the {@code version} Maven coordinate of the artifact.
	 *
	 * @return the {@code version} Maven coordinate, can't be {@literal null}
	 */
	Version version();

	/**
	 * Formats the Artifact coordinates into a textual representation.
	 *
	 * @return the textual representation for the coordinates, can't be {@literal null}
	 */
	default String format() {
		return format(groupId(), artifactId(), version());
	}

	@Override
	default int compareTo(ArtifactCoordinates o) {
		return compare(this, o);
	}
}
