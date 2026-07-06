package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface that represents the primary entry point to the {@code Artifactory} domain.
 * <p>
 * The {@code Artifactory} service provides operations for resolving, inspecting, and publishing
 * versioned artifacts managed by the platform. Artifacts represent reusable configuration libraries
 * whose metadata and property definitions are versioned and published through this service.
 * <p>
 * Implementations are responsible for enforcing domain invariants such as coordinate uniqueness,
 * release immutability, and metadata validation. Published artifacts are expected to be immutable
 * representations of a configuration schema at a specific point in time.
 * <p>
 * The {@code Artifactory} interface represents the main boundary between the application layer and
 * the artifact repository domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface Artifactory {

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
	 * Returns the subset of the given {@link ArtifactCoordinates} that are already indexed by this
	 * {@code Artifactory}.
	 * <p>
	 * This is a batch counterpart to {@link #exists(ArtifactCoordinates)}, intended for callers that
	 * need to resolve the existence of many coordinates at once without issuing one query per coordinate.
	 * <p>
	 * Coordinates from the input that have no matching artifact version indexed by this {@code Artifactory}
	 * are simply omitted from the returned set — they are not reported back as missing or invalid, since
	 * "not yet indexed" is an expected, non-exceptional outcome for a batch existence check. Callers that
	 * need to know which of their coordinates are absent should compute the difference against the input.
	 *
	 * @param coordinates coordinates to check, can be empty
	 * @return the subset of the given coordinates that exist, never {@literal null}, empty if the input is empty
	 */
	@NonNull
	Set<ArtifactCoordinates> existing(@NonNull Collection<ArtifactCoordinates> coordinates);

	/**
	 * Publishes a new artifact version based on the provided metadata.
	 * <p>
	 * This operation performs the release process for an artifact, which includes:
	 * <ul>
	 *   <li>Validating artifact coordinates and metadata</li>
	 *   <li>Persisting the artifact version</li>
	 *   <li>Registering associated property definitions</li>
	 *   <li>Emitting a domain event indicating that a new artifact version has been released</li>
	 * </ul>
	 * <p>
	 * Implementations should treat published artifact versions as immutable. Once a version has been
	 * successfully published, its metadata and property definitions must not be modified.
	 * <p>
	 * The {@code artifactory.artifact-version.publication-created} domain event will be emitted after
	 * a successful publication.
	 * <p>
	 * Publishing is restricted to owners that hold an active verification claim covering the artifact
	 * {@code groupId}. The {@code ownerId} identifies the publishing namespace and is resolved to its
	 * owner before the claim is checked.
	 *
	 * @param ownerId the identifier of the namespace publishing the artifact, can't {@literal null}
	 * @param metadata the metadata describing the artifact version to publish, can't {@literal null}
	 * @return the resulting {@link VersionedArtifact} representing the published artifact
	 * @throws ArtifactVersionExistsException when an artifact with the same coordinates already exists
	 * @throws OwnerNotFoundException when the owner cannot be resolved
	 *         from the supplied {@code ownerId}
	 * @throws com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException when the owner does not hold
	 *         an active verification claim covering the artifact {@code groupId}
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-version.publication-created")
	VersionedArtifact publish(@NonNull EntityId ownerId, @NonNull ArtifactMetadata metadata);

}
