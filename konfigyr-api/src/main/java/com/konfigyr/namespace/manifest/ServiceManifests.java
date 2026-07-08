package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.ArtifactMetadata;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.ServiceRelease;
import com.konfigyr.artifactory.ServiceReleaseCandidate;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface that defines a contract for building a service's Service Configuration Manifest through
 * the resolve/upload/complete protocol used by build plugins.
 * <p>
 * Implementations are responsible for enforcing the domain invariants of a single-row-per-service
 * {@link ServiceRelease}: taking over an in-progress build rather than rejecting it, tracking which
 * declared artifacts still need their metadata uploaded, and refusing to complete a release while any
 * declared artifact remains unresolved.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ServiceRelease
 **/
public interface ServiceManifests {

	/**
	 * Returns the current {@link Manifest} associated with the given service.
	 * <p>
	 * A manifest represents the set of {@link com.konfigyr.artifactory.ArtifactCoordinates artifacts}
	 * that are currently used by a specific service within a {@link com.konfigyr.namespace.Namespace}.
	 * Each artifact referenced by the manifest contributes configuration metadata resolved through the
	 * {@code Artifactory} domain.
	 * <p>
	 * The manifest acts as the bridge between a service and the configuration metadata provided by its
	 * dependencies. When a manifest is resolved, the Artifactory aggregates the configuration property
	 * definitions contributed by all referenced artifacts and produces the effective configuration
	 * metadata used by the service.
	 *
	 * @param service service for which the manifest should be retrieved, can't be {@literal null}.
	 * @return the current {@link Manifest} for the service, never {@literal null}.
	 */
	@NonNull
	Manifest get(@NonNull Service service);

	/**
	 * Opens a new build for the given service, or takes over its current {@link ServiceRelease} if one
	 * is already in progress, there is only ever one pending release row per service.
	 * <p>
	 * Every declared candidate is resolved against artifacts already uploaded in this build and against
	 * the Artifactory index, producing one {@link com.konfigyr.artifactory.ServiceReleaseEntry} per
	 * candidate with its resolved {@link com.konfigyr.artifactory.ArtifactUploadStatus}. Declared
	 * artifacts that are no longer part of the given {@code artifacts} collection are pruned.
	 *
	 * @param service the service opening or taking over a build, can't be {@literal null}.
	 * @param artifacts the artifact coordinates and checksums resolved locally by the build plugin, can't be {@literal null}.
	 * @return the resolved service release, never {@literal null}.
	 */
	@NonNull
	ServiceRelease open(@NonNull Service service, @NonNull Collection<ServiceReleaseCandidate> artifacts);

	/**
	 * Retrieves a previously opened or completed {@link ServiceRelease} by its identifier.
	 * <p>
	 * Returns the same shape {@link #open} and {@link #complete} already return: current
	 * {@link com.konfigyr.artifactory.ReleaseState}, one {@link com.konfigyr.artifactory.ServiceReleaseEntry}
	 * per declared artifact with its current {@link com.konfigyr.artifactory.ArtifactUploadStatus}, and,
	 * for a {@link com.konfigyr.artifactory.ReleaseState#FAILED} release, the coordinates that were never
	 * uploaded.
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release to retrieve, can't be {@literal null}.
	 * @return the matching service release, or {@literal empty} if no release with this identifier
	 *         exists for this service.
	 */
	@NonNull
	Optional<ServiceRelease> get(@NonNull Service service, @NonNull EntityId releaseId);

	/**
	 * Records the Spring Boot configuration metadata uploaded by the build plugin for a single artifact
	 * declared in the current build, exploding its property descriptors into the service configuration
	 * catalog.
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release the upload belongs to, can't be {@literal null}.
	 * @param metadata the uploaded artifact metadata, can't be {@literal null}.
	 */
	void upload(@NonNull Service service, @NonNull EntityId releaseId, @NonNull ArtifactMetadata metadata);

	/**
	 * Completes the given build: validates that every declared artifact was uploaded, populates the
	 * service configuration catalog for artifacts sourced from the Artifactory, and transitions the
	 * release to {@link com.konfigyr.artifactory.ReleaseState#RELEASED} — or to
	 * {@link com.konfigyr.artifactory.ReleaseState#FAILED} if any declared artifact was never uploaded.
	 * <p>
	 * Once the release transitions to {@link com.konfigyr.artifactory.ReleaseState#RELEASED}, a
	 * {@link com.konfigyr.namespace.ServiceEvent.Released} event is published.
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release to complete, can't be {@literal null}.
	 * @return the completed service release, never {@literal null}.
	 */
	@NonNull
	ServiceRelease complete(@NonNull Service service, @NonNull EntityId releaseId);

}
