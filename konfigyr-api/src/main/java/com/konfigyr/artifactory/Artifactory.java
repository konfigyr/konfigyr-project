package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface that represents a visibility-based, read-only view onto the {@code Artifactory} domain.
 * <p>
 * The {@code Artifactory} service resolves and inspects versioned artifacts managed by the platform,
 * answering "what can this caller see": {@link ArtifactVisibility#PUBLIC PUBLIC} artifacts are visible
 * to everyone, {@link ArtifactVisibility#PRIVATE PRIVATE} artifacts only to their owner.
 * <p>
 * Managing a namespace's own registry — publishing new versions, changing visibility, or removing what
 * it has published — is handled by {@link Publications} instead.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Publications
 */
public interface Artifactory {

	/**
	 * Resolves a specific artifact definition identified by the provided artifact key.
	 * <p>
	 * If an artifact with the given {@link ArtifactKey} exists, the corresponding {@link ArtifactDefinition}
	 * is returned. Otherwise, an empty {@link Optional} is returned.
	 * <p>
	 * This operation is typically used by consumers that need to retrieve the information about the
	 * artifact that is already indexed by this {@code Artifactory}.
	 *
	 * @param key the artifact key identifying the artifact, can't {@literal null}
	 * @return the resolved {@link ArtifactDefinition}, or {@code empty} if no artifact exists for the given key
	 */
	@NonNull
	Optional<ArtifactDefinition> get(@NonNull ArtifactKey key);

	/**
	 * Resolves a specific artifact definition identified by the provided key, restricted to
	 * what the given {@code owner} is allowed to see.
	 * <p>
	 * A {@link ArtifactVisibility#PUBLIC PUBLIC} artifact is always resolved. A
	 * {@link ArtifactVisibility#PRIVATE PRIVATE} artifact is only resolved when {@code owner} is its
	 * owning namespace; otherwise this behaves as if the coordinates did not exist, so that an
	 * unauthorized caller can't distinguish "private, not yours" from "never existed".
	 *
	 * @param owner the namespace on whose behalf this lookup is performed, or {@literal null} if the
	 *        caller has no namespace context, in which case only {@code PUBLIC} artifacts resolve
	 * @param key the artifact key identifying the artifact version, can't {@literal null}
	 * @return the resolved {@link ArtifactDefinition}, or {@code empty} if none exists or is visible to {@code owner}
	 */
	@NonNull
	Optional<ArtifactDefinition> get(@Nullable Owner owner, @NonNull ArtifactKey key);

	/**
	 * Determines whether an artifact definition exists for the given artifact key.
	 * <p>
	 * This is a lightweight existence check intended for validation and conditional logic where loading
	 * the full {@link ArtifactDefinition} would be unnecessary.
	 *
	 * @param key the artifact key identifying the artifact, can't {@literal null}
	 * @return {@code true} if an artifact definition exists, otherwise {@code false}
	 */
	boolean exists(@NonNull ArtifactKey key);

	/**
	 * Determines whether an artifact definition exists for the given key and is visible to the
	 * given {@code owner}, see {@link #get(Owner, ArtifactKey)} for the visibility rule.
	 *
	 * @param owner the namespace on whose behalf this check is performed, or {@literal null} if the
	 *        caller has no namespace context, in which case only {@code PUBLIC} artifacts match
	 * @param key the artifact key identifying the artifact, can't {@literal null}
	 * @return {@code true} if an artifact definition exists and is visible to {@code owner}, otherwise {@code false}
	 */
	boolean exists(@Nullable Owner owner, @NonNull ArtifactKey key);

	/**
	 * Resolves a specific artifact version identified by the provided coordinates.
	 * <p>
	 * If an artifact with the given {@link ArtifactCoordinates} exists, the corresponding {@link VersionedArtifact}
	 * is returned. Otherwise, an empty {@link Optional} is returned.
	 * <p>
	 * This operation is typically used by consumers that need to retrieve the full metadata representation
	 * of a released artifact version.
	 *
	 * @param coordinates the artifact coordinates identifying the artifact version, can't {@literal null}
	 * @return the resolved {@link VersionedArtifact}, or {@code empty} if no artifact exists for the given coordinates
	 */
	@NonNull
	Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates);

	/**
	 * Resolves a specific artifact version identified by the provided coordinates, restricted to
	 * what the given {@code owner} is allowed to see.
	 * <p>
	 * A {@link ArtifactVisibility#PUBLIC PUBLIC} artifact is always resolved. A
	 * {@link ArtifactVisibility#PRIVATE PRIVATE} artifact is only resolved when {@code owner} is its
	 * owning namespace; otherwise this behaves as if the coordinates did not exist, so that an
	 * unauthorized caller can't distinguish "private, not yours" from "never existed".
	 *
	 * @param owner the namespace on whose behalf this lookup is performed, or {@literal null} if the
	 *        caller has no namespace context, in which case only {@code PUBLIC} artifacts resolve
	 * @param coordinates the artifact coordinates identifying the artifact version, can't {@literal null}
	 * @return the resolved {@link VersionedArtifact}, or {@code empty} if none exists or is visible to {@code owner}
	 */
	@NonNull
	Optional<VersionedArtifact> get(@Nullable Owner owner, @NonNull ArtifactCoordinates coordinates);

	/**
	 * Returns the property definitions associated with the specified artifact version.
	 * <p>
	 * Property definitions describe the configuration schema exposed by the artifact. Each definition specifies
	 * the property name, type, and JSON schema used to validate property values.
	 * <p>
	 * The returned list represents the complete set of properties defined for the artifact version identified
	 * by the provided coordinates.
	 *
	 * @param coordinates the artifact coordinates identifying the artifact version, can't {@literal null}
	 * @return the list of property definitions associated with the artifact, never {@literal null}
	 * @throws ArtifactVersionNotFoundException if no artifact exists for the given coordinates
	 */
	@NonNull
	List<PropertyDefinition> properties(@NonNull ArtifactCoordinates coordinates);

	/**
	 * Determines whether an artifact version exists for the given coordinates.
	 * <p>
	 * This is a lightweight existence check intended for validation and conditional logic where loading
	 * the full {@link VersionedArtifact} would be unnecessary.
	 *
	 * @param coordinates the artifact coordinates identifying the artifact version, can't {@literal null}
	 * @return {@code true} if an artifact version exists, otherwise {@code false}
	 */
	boolean exists(@NonNull ArtifactCoordinates coordinates);

	/**
	 * Determines whether an artifact version exists for the given coordinates and is visible to the
	 * given {@code owner}, see {@link #get(Owner, ArtifactCoordinates)} for the visibility rule.
	 *
	 * @param owner the namespace on whose behalf this check is performed, or {@literal null} if the
	 *        caller has no namespace context, in which case only {@code PUBLIC} artifacts match
	 * @param coordinates the artifact coordinates identifying the artifact version, can't {@literal null}
	 * @return {@code true} if an artifact version exists and is visible to {@code owner}, otherwise {@code false}
	 */
	boolean exists(@Nullable Owner owner, @NonNull ArtifactCoordinates coordinates);

	/**
	 * Returns the {@link Publication} for each of the given {@link ArtifactCoordinates} that is already
	 * indexed by this {@code Artifactory}.
	 * <p>
	 * This is a batch counterpart to {@link #exists(ArtifactCoordinates)}, intended for callers that
	 * need to resolve the existence of many coordinates at once without issuing one query per coordinate.
	 * Returning the {@link Publication} rather than just the matched coordinate lets callers read its
	 * {@link Publication#checksum()} directly, without a further lookup.
	 * <p>
	 * Coordinates from the input that have no matching artifact version indexed by this {@code Artifactory}
	 * are simply omitted from the returned set — they are not reported back as missing or invalid, since
	 * "not yet indexed" is an expected, non-exceptional outcome for a batch existence check. Callers that
	 * need to know which of their coordinates are absent should compute the difference against the input.
	 * <p>
	 * Only {@link ArtifactVisibility#PUBLIC PUBLIC} artifacts, and {@link ArtifactVisibility#PRIVATE PRIVATE}
	 * artifacts owned by the given {@code owner}, are considered — a match that is {@code PRIVATE} to a
	 * different namespace is treated the same as "not yet indexed" and omitted from the result.
	 *
	 * @param owner the namespace on whose behalf this batch check is performed, can't be {@literal null}
	 * @param coordinates coordinates to check, can be empty
	 * @return the publications matching the given coordinates that exist, never {@literal null}, empty if the input is empty
	 */
	@NonNull
	Set<Publication> existing(@NonNull Owner owner, @NonNull Collection<ArtifactCoordinates> coordinates);

}
