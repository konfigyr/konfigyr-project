package com.konfigyr.artifactory;

import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Interface that represents a namespace's own view onto the artifacts it manages within the
 * {@code Artifactory} domain.
 * <p>
 * Where {@link Artifactory} answers "what can this caller see", a visibility-based view in which
 * {@link ArtifactVisibility#PUBLIC PUBLIC} artifacts are visible to everyone and
 * {@link ArtifactVisibility#PRIVATE PRIVATE} artifacts are visible only to their owner, {@code Publications}
 * answers "what does this namespace own". Every operation on this interface is strictly scoped to the
 * given {@link Owner}: a {@code PUBLIC} artifact owned by a different namespace is never returned, listed,
 * or matched here, even though it would be visible through {@link Artifactory}.
 * <p>
 * Use {@link Artifactory} when resolving or consuming artifacts published by any namespace. Use
 * {@code Publications} when managing a namespace's own registry: listing its artifacts, publishing new
 * versions, changing visibility, or removing what it has published.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface Publications {

	/**
	 * Searches the {@link ArtifactDefinition artifact definitions} owned by the given namespace.
	 * <p>
	 * Unlike {@link Artifactory}, this is not a visibility-based lookup, only artifacts owned by
	 * {@code owner} are matched, regardless of their {@link ArtifactVisibility}.
	 *
	 * @param owner the namespace whose artifacts are being searched, can't be {@literal null}
	 * @param query the search query used to filter and page through the results, can't be {@literal null}
	 * @return a page of {@link ArtifactDefinition artifact definitions} owned by {@code owner}, never {@literal null}
	 */
	Page<ArtifactDefinition> artifacts(Owner owner, SearchQuery query);

	/**
	 * Resolves the overview of an artifact owned by the given namespace.
	 * <p>
	 * Unlike {@link Artifactory#get(Owner, ArtifactKey)}, this lookup is strict: it only resolves when
	 * {@code owner} is the actual owner of the artifact. A {@code PUBLIC} artifact owned by a different
	 * namespace behaves as if it did not exist.
	 *
	 * @param owner the namespace expected to own the artifact, can't be {@literal null}
	 * @param key the artifact key identifying the artifact, can't be {@literal null}
	 * @return the resolved {@link ArtifactDefinition}, or {@code empty} if no such artifact is owned by {@code owner}
	 */
	Optional<ArtifactDefinition> get(Owner owner, ArtifactKey key);

	/**
	 * Determines whether the given namespace owns an artifact for the given key, see
	 * {@link #get(Owner, ArtifactKey)} for the ownership rule.
	 *
	 * @param owner the namespace expected to own the artifact, can't be {@literal null}
	 * @param key the artifact key identifying the artifact, can't be {@literal null}
	 * @return {@code true} if {@code owner} owns an artifact for the given key, otherwise {@code false}
	 */
	boolean exists(Owner owner, ArtifactKey key);

	/**
	 * Permanently removes the artifact identified by the given key, together with every
	 * {@link VersionedArtifact} published under it, from the registry.
	 * <p>
	 * This is a destructive, non-reversible operation. Only the namespace that owns the artifact may
	 * deregister it. The {@code artifactory.artifact-definition.deregistered} domain event is emitted
	 * once the artifact and all of its versions have been removed.
	 *
	 * @param owner the namespace requesting the removal, can't be {@literal null}
	 * @param key the {@code groupId}/{@code artifactId} identity of the artifact to remove, can't be {@literal null}
	 * @throws ArtifactDefinitionNotFoundException when no artifact exists for the given key
	 * @throws ArtifactOwnershipMismatchException when the given owner does not own the artifact
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-definition.deregistered")
	void deregister(Owner owner, ArtifactKey key);

	/**
	 * Searches the {@link VersionedArtifact versions} published under artifacts owned by the given namespace.
	 * <p>
	 * Scope this search to a single artifact by supplying {@link ArtifactKey#GROUP_ID_CRITERIA},
	 * {@link ArtifactKey#ARTIFACT_ID_CRITERIA} or {@link ArtifactCoordinates#VERSION_CRITERIA} in the {@code query}.
	 * <p>
	 * As with every other method on this interface, only versions of artifacts owned by {@code owner} are matched.
	 *
	 * @param owner the namespace whose artifact versions are being searched, can't be {@literal null}
	 * @param query the search query used to filter and page through the results, can't be {@literal null}
	 * @return a page of {@link VersionedArtifact versions} owned by {@code owner}, never {@literal null}
	 */
	Page<VersionedArtifact> versions(Owner owner, SearchQuery query);

	/**
	 * Resolves a specific artifact version owned by the given namespace.
	 * <p>
	 * Unlike {@link Artifactory#get(Owner, ArtifactCoordinates)}, this lookup is strict: it only resolves
	 * when {@code owner} is the actual owner of the artifact version. A {@code PUBLIC} version owned by a
	 * different namespace behaves as if it did not exist.
	 *
	 * @param owner the namespace expected to own the artifact version, can't be {@literal null}
	 * @param coordinates the artifact coordinates identifying the artifact version, can't be {@literal null}
	 * @return the resolved {@link VersionedArtifact}, or {@code empty} if no such version is owned by {@code owner}
	 */
	Optional<VersionedArtifact> get(Owner owner, ArtifactCoordinates coordinates);

	/**
	 * Determines whether the given namespace owns an artifact version for the given coordinates, see
	 * {@link #get(Owner, ArtifactCoordinates)} for the ownership rule.
	 *
	 * @param owner the namespace expected to own the artifact version, can't be {@literal null}
	 * @param coordinates the artifact coordinates identifying the artifact version, can't be {@literal null}
	 * @return {@code true} if {@code owner} owns an artifact version for the given coordinates, otherwise
	 *         {@code false}
	 */
	boolean exists(Owner owner, ArtifactCoordinates coordinates);

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
	 * {@code groupId}. The caller is expected to have already resolved the publishing namespace to
	 * its {@link Owner}.
	 *
	 * @param owner the namespace publishing the artifact, can't be {@literal null}
	 * @param metadata the metadata describing the artifact version to publish, can't be {@literal null}
	 * @return the resulting {@link VersionedArtifact} representing the published artifact
	 * @throws ArtifactVersionExistsException when an artifact with the same coordinates already exists
	 * @throws ArtifactOwnershipMismatchException when the artifact already exists and is owned by a
	 *         different namespace
	 * @throws com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException when the owner does not hold
	 *         an active verification claim covering the artifact {@code groupId}
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-version.publication-created")
	VersionedArtifact publish(Owner owner, ArtifactMetadata metadata);

	/**
	 * Retracts a single previously published artifact version, removing it from the registry.
	 * <p>
	 * Unlike {@link #deregister(Owner, ArtifactKey)}, this only removes the one version identified by
	 * {@code coordinates} — the artifact definition and any other published versions are unaffected.
	 * This is a destructive, non-reversible operation. Only the namespace that owns the artifact may
	 * retract one of its versions. The {@code artifactory.artifact-version.publication-retracted} domain
	 * event is emitted once the version has been removed.
	 *
	 * @param owner the namespace requesting the removal, can't be {@literal null}
	 * @param coordinates the artifact coordinates identifying the version to remove, can't be {@literal null}
	 * @throws ArtifactVersionNotFoundException when no artifact version exists for the given coordinates
	 * @throws ArtifactOwnershipMismatchException when the given owner does not own the artifact
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-version.publication-retracted")
	void retract(Owner owner, ArtifactCoordinates coordinates);

	/**
	 * Changes the {@link ArtifactVisibility} of the artifact identified by the given {@link ArtifactKey}.
	 * <p>
	 * Visibility is a {@code groupId}/{@code artifactId} level concern, not a version level one: it
	 * applies to every {@link VersionedArtifact} published under those coordinates. Only the
	 * namespace that owns the artifact may change its visibility. The
	 * {@code artifactory.artifact-definition.visibility-changed} domain event is emitted after a
	 * successful change.
	 *
	 * @param owner the namespace requesting the change, can't be {@literal null}
	 * @param key the {@code groupId}/{@code artifactId} identity of the artifact, can't be {@literal null}
	 * @param visibility the visibility to apply, can't be {@literal null}
	 * @throws ArtifactDefinitionNotFoundException when no artifact exists for the given coordinates
	 * @throws ArtifactOwnershipMismatchException when the given owner does not own the artifact
	 */
	@DomainEventPublisher(publishes = "artifactory.artifact-definition.visibility-changed")
	void changeVisibility(Owner owner, ArtifactKey key, ArtifactVisibility visibility);

}
