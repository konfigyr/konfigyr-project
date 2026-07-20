package com.konfigyr.artifactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.konfigyr.support.SearchQuery;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Interface that describes the versionless Maven coordinate pair, {@code groupId:artifactId}, that
 * uniquely identifies an artifact across all of its published versions.
 * <p>
 * This is the "GA" half of the full "GAV" ({@code groupId:artifactId:version}) coordinate system used by
 * {@link ArtifactCoordinates}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ArtifactCoordinates
 */
@NullMarked
public interface ArtifactKey extends Serializable {

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of artifacts
	 * or property descriptors by their {@link ArtifactKey groupId:artifactId pair}.
	 */
	SearchQuery.Criteria<ArtifactKey> CRITERIA = SearchQuery.criteria("artifact.key", ArtifactKey.class);

	/**
	 * Parses the given textual representation of a Maven {@code groupId:artifactId} pair and creates an
	 * {@link ArtifactKey} instance.
	 * <p>
	 * The expected format is {@code groupId:artifactId}.
	 *
	 * @param key the textual representation of the key to parse, can be {@literal null}
	 * @return the parsed artifact key, never {@literal null}
	 * @throws IllegalArgumentException if the key string is invalid or cannot be parsed
	 */
	@JsonCreator
	static ArtifactKey parse(@Nullable String key) {
		return SimpleArtifactKey.parse(key);
	}

	/**
	 * Creates an {@link ArtifactKey} instance from the given Maven coordinate components.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @return the artifact key, never {@literal null}
	 */
	static ArtifactKey of(String groupId, String artifactId) {
		return new SimpleArtifactKey(groupId, artifactId);
	}

	/**
	 * Returns the {@code groupId} Maven coordinate of the artifact.
	 *
	 * @return the {@code groupId} Maven coordinate, can't be {@literal null}
	 */
	String groupId();

	/**
	 * Returns the {@code artifactId} Maven coordinate of the artifact.
	 *
	 * @return the {@code artifactId} Maven coordinate, can't be {@literal null}
	 */
	String artifactId();

	/**
	 * Formats the {@code groupId}/{@code artifactId} pair into a textual representation.
	 *
	 * @return the formatted key in the format {@code groupId:artifactId}, never {@literal null}
	 */
	default String format() {
		return format(groupId(), artifactId());
	}

	/**
	 * Formats the given Maven coordinate components into a textual representation.
	 *
	 * @param groupId    the group identifier, can't be {@literal null}
	 * @param artifactId the artifact identifier, can't be {@literal null}
	 * @return the formatted key in the format {@code groupId:artifactId}, never {@literal null}
	 */
	static String format(String groupId, String artifactId) {
		return "%s:%s".formatted(groupId, artifactId);
	}

}
