package com.konfigyr.artifactory;

import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Interface that represents the primary entry point to the {@code Artifactory} domain.
 * <p>
 * The {@code Artifactory} service provides operations for resolving, inspecting, and publishing
 * versioned artifacts managed by the platform. Artifacts represent reusable configuration libraries
 * whose metadata and property definitions are versioned and released through this service.
 * <p>
 * Implementations are responsible for enforcing domain invariants such as coordinate uniqueness,
 * release immutability, and metadata validation. Released artifacts are expected to be immutable
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
	 * Searches for {@link PropertyDefinition property definitions} matching the given query.
	 * <p>
	 * The search considers the property {@link PropertyDefinition#name() name} and
	 * {@link PropertyDefinition#description() description} fields against the
	 * {@link SearchQuery#TERM} criteria. Callers may additionally narrow results by providing:
	 * <ul>
	 *   <li>{@link PropertyDefinition#ARTIFACT_CRITERIA}: restricts results to properties
	 *       contributed by the artifact version identified by those coordinates</li>
	 *   <li>{@link PropertyDefinition#INCLUDE_DEPRECATED_CRITERIA}: when {@code true}, includes
	 *       properties that carry {@link Deprecation} metadata; defaults to {@code false}</li>
	 * </ul>
	 *
	 * @param query the search query containing filter criteria and pagination hints, never {@literal null}
	 * @return a page of matching property definitions, never {@literal null}
	 */
	@NonNull
	Page<PropertyDefinition> search(@NonNull SearchQuery query);

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
	 * Implementations should treat released artifact versions as immutable. Once a version has been
	 * successfully published, its metadata and property definitions must not be modified.
	 * <p>
	 * The {@code artifactory.artifact-version.release} domain event will be emitted after
	 * a successful release.
	 *
	 * @param metadata the metadata describing the artifact version to release, can't {@literal null}
	 * @return the resulting {@link VersionedArtifact} representing the released artifact
	 * @throws ArtifactVersionExistsException when an artifact with the same coordinates already exists
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-version.release")
	VersionedArtifact release(@NonNull ArtifactMetadata metadata);

}
