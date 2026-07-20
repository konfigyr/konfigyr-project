package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;

/**
 * Abstract event type that should be used for all {@code Artifactory} Domain related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class ArtifactoryEvent extends EntityEvent
		permits ArtifactoryEvent.ArtifactEvent, ArtifactoryEvent.DefinitionEvent, ArtifactoryEvent.OwnershipTransferEvent {

	private final Owner owner;

	protected ArtifactoryEvent(EntityId id, @NonNull Owner owner) {
		super(id);
		this.owner = owner;
	}

	/**
	 * The owner of the {@code Artifact} that was the subject of the event.
	 *
	 * @return the artifact owner, never {@literal null}.
	 */
	@NonNull
	public Owner owner() {
		return this.owner;
	}

	/**
	 * Abstract event type used for events related to a {@link VersionedArtifact}.
	 */
	public static sealed abstract class ArtifactEvent extends ArtifactoryEvent
			permits PublicationCreated, PublicationCompleted, PublicationFailed, PublicationRetracted {

		private final ArtifactCoordinates coordinates;

		/**
		 * Create a new {@link ArtifactEvent} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact artifact version to create the event for, can't be {@literal null}.
		 */
		protected ArtifactEvent(@NonNull VersionedArtifact artifact) {
			this(artifact.id(), artifact.owner(), artifact.coordinates());
		}

		/**
		 * Create a new {@link ArtifactEvent} event with the {@link EntityId entity identifier}
		 * of the {@link VersionedArtifact} and it's coordinates.
		 *
		 * @param id entity identifier of the created artifact version, can't be {@literal null}.
		 * @param owner the owner of the created artifact version, can't be {@literal null}.
		 * @param coordinates the artifact coordinates of the created artifact version, can't be {@literal null}.
		 */
		protected ArtifactEvent(EntityId id, @NonNull Owner owner, @NonNull ArtifactCoordinates coordinates) {
			super(id, owner);
			this.coordinates = coordinates;
		}

		/**
		 * Returns the {@link ArtifactCoordinates} of the affected {@link VersionedArtifact}.
		 *
		 * @return artifact coordinates, never {@literal null}.
		 */
		@NonNull
		public ArtifactCoordinates coordinates() {
			return coordinates;
		}
	}

	/**
	 * Event that would be published when a new {@link VersionedArtifact} is created.
	 */
	@DomainEvent(name = "artifact-version.publication-created", namespace = "artifactory")
	public static final class PublicationCreated extends ArtifactEvent {

		/**
		 * Create a new {@link PublicationCreated} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was just created by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the created artifact version
		 * @param owner the owner of the created artifact version that was just published
		 * @param coordinates the artifact coordinates of the created artifact version
		 */
		public PublicationCreated(EntityId id, Owner owner, ArtifactCoordinates coordinates) {
			super(id, owner, coordinates);
		}
	}

	/**
	 * Event that would be published when a new {@link VersionedArtifact} is successfully published.
	 */
	@DomainEvent(name = "artifact-version.publication-completed", namespace = "artifactory")
	public static final class PublicationCompleted extends ArtifactEvent {

		/**
		 * Create a new {@link PublicationCompleted} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact affected artifact version, can't be {@literal null}.
		 */
		public PublicationCompleted(VersionedArtifact artifact) {
			super(artifact);
		}

		/**
		 * Create a new {@link PublicationCompleted} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was successfully published by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the artifact version
		 * @param owner the owner of the artifact version
		 * @param coordinates the artifact coordinates of the artifact version
		 */
		public PublicationCompleted(EntityId id, Owner owner, ArtifactCoordinates coordinates) {
			super(id, owner, coordinates);
		}
	}

	/**
	 * Event that would be published when a publication for {@link VersionedArtifact} failed.
	 */
	@DomainEvent(name = "artifact-version.publication-failed", namespace = "artifactory")
	public static final class PublicationFailed extends ArtifactEvent {

		/**
		 * Create a new {@link PublicationFailed} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact affected artifact version, can't be {@literal null}.
		 */
		public PublicationFailed(VersionedArtifact artifact) {
			super(artifact);
		}

		/**
		 * Create a new {@link PublicationFailed} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was could not be published by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the artifact version
		 * @param owner the owner of the artifact version
		 * @param coordinates the artifact coordinates of the artifact version
		 */
		public PublicationFailed(EntityId id, Owner owner, ArtifactCoordinates coordinates) {
			super(id, owner, coordinates);
		}
	}

	/**
	 * Event that would be published when a previously published {@link VersionedArtifact} is retracted.
	 */
	@DomainEvent(name = "artifact-version.publication-retracted", namespace = "artifactory")
	public static final class PublicationRetracted extends ArtifactEvent {

		/**
		 * Create a new {@link PublicationRetracted} event for the {@link VersionedArtifact} that was retracted.
		 *
		 * @param id entity identifier of the retracted artifact version, can't be {@literal null}.
		 * @param owner the owner of the retracted artifact version
		 * @param coordinates the artifact coordinates of the retracted artifact version, can't be {@literal null}.
		 */
		public PublicationRetracted(EntityId id, @NonNull Owner owner, @NonNull ArtifactCoordinates coordinates) {
			super(id, owner, coordinates);
		}
	}

	/**
	 * Abstract event type used for events related to an {@link ArtifactDefinition}.
	 */
	public static sealed abstract class DefinitionEvent extends ArtifactoryEvent
			permits Deregistered, VisibilityChanged {

		private final ArtifactKey key;

		/**
		 * Create a new {@link DefinitionEvent} event for the {@link ArtifactDefinition}.
		 *
		 * @param id entity identifier of the affected artifact definition, can't be {@literal null}.
		 * @param owner the owner of the affected artifact definition
		 * @param key the {@code groupId}/{@code artifactId} identity of the affected artifact, can't be {@literal null}.
		 */
		protected DefinitionEvent(EntityId id, @NonNull Owner owner, @NonNull ArtifactKey key) {
			super(id, owner);
			this.key = key;
		}

		/**
		 * Returns the {@link ArtifactKey} of the affected {@link ArtifactDefinition}.
		 *
		 * @return artifact key, never {@literal null}.
		 */
		@NonNull
		public ArtifactKey key() {
			return key;
		}
	}

	/**
	 * Event that would be published when an {@link ArtifactDefinition}, together with every
	 * {@link VersionedArtifact} published under it, is deregistered from the registry.
	 */
	@DomainEvent(name = "artifact-definition.deregistered", namespace = "artifactory")
	public static final class Deregistered extends DefinitionEvent {

		/**
		 * Create a new {@link Deregistered} event for the {@link ArtifactDefinition} that was removed.
		 *
		 * @param id entity identifier of the deregistered artifact definition, can't be {@literal null}.
		 * @param owner the owner of the deregistered artifact definition
		 * @param key the {@code groupId}/{@code artifactId} identity of the deregistered artifact, can't be {@literal null}.
		 */
		public Deregistered(EntityId id, @NonNull Owner owner, @NonNull ArtifactKey key) {
			super(id, owner, key);
		}
	}

	/**
	 * Event that would be published when the {@link ArtifactVisibility} of an {@link ArtifactDefinition} changes.
	 */
	@DomainEvent(name = "artifact-definition.visibility-changed", namespace = "artifactory")
	public static final class VisibilityChanged extends DefinitionEvent {

		private final ArtifactVisibility visibility;

		/**
		 * Create a new {@link VisibilityChanged} event for the {@link ArtifactDefinition} whose visibility changed.
		 *
		 * @param id entity identifier of the affected artifact definition, can't be {@literal null}.
		 * @param owner the owner of the affected artifact definition
		 * @param key the {@code groupId}/{@code artifactId} identity of the affected artifact, can't be {@literal null}.
		 * @param visibility the {@link ArtifactVisibility} that was applied, can't be {@literal null}.
		 */
		public VisibilityChanged(EntityId id, @NonNull Owner owner, @NonNull ArtifactKey key, @NonNull ArtifactVisibility visibility) {
			super(id, owner, key);
			this.visibility = visibility;
		}

		/**
		 * Returns the {@link ArtifactVisibility} that was applied to the affected {@link ArtifactDefinition}.
		 *
		 * @return the new visibility, never {@literal null}.
		 */
		@NonNull
		public ArtifactVisibility visibility() {
			return visibility;
		}
	}

	/**
	 * Abstract event type used for events related to the resolution of an
	 * {@code com.konfigyr.artifactory.transfer.ArtifactOwnershipTransfer}.
	 */
	public static sealed abstract class OwnershipTransferEvent extends ArtifactoryEvent
			permits OwnershipTransferAccepted, OwnershipTransferRejected, OwnershipTransferCancelled {

		private final String groupId;
		private final Owner from;
		private final Owner to;

		/**
		 * Create a new {@link OwnershipTransferEvent} event for the given resolved transfer request.
		 *
		 * @param id the entity identifier of the resolved transfer request, can't be {@literal null}.
		 * @param groupId the artifact groupId coordinate the transfer request pertains to, can't be {@literal null}.
		 * @param from the namespace that owns the affected artifacts, can't be {@literal null}.
		 * @param to the namespace that requested ownership of the affected artifacts, can't be {@literal null}.
		 */
		protected OwnershipTransferEvent(EntityId id, @NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
			super(id, from);
			this.groupId = groupId;
			this.from = from;
			this.to = to;
		}

		/**
		 * Returns the Maven {@code groupId} coordinate the transfer request pertains to.
		 *
		 * @return artifact groupId coordinate, never {@literal null}.
		 */
		@NonNull
		public String groupId() {
			return groupId;
		}

		/**
		 * Returns the namespace that owns the affected artifacts.
		 *
		 * @return current owner, never {@literal null}.
		 */
		@NonNull
		public Owner from() {
			return from;
		}

		/**
		 * Returns the namespace that requested ownership of the affected artifacts.
		 *
		 * @return requesting namespace, never {@literal null}.
		 */
		@NonNull
		public Owner to() {
			return to;
		}
	}

	/**
	 * Event that would be published when an artifact ownership transfer request is accepted by the
	 * current owner and ownership of the affected artifacts has moved.
	 */
	@DomainEvent(name = "ownership-transfer.accepted", namespace = "artifactory")
	public static final class OwnershipTransferAccepted extends OwnershipTransferEvent {

		/**
		 * Create a new {@link OwnershipTransferAccepted} event for the given accepted transfer request.
		 *
		 * @param id the entity identifier of the accepted transfer request, can't be {@literal null}.
		 * @param groupId the artifact groupId coordinate whose artifacts were transferred, can't be {@literal null}.
		 * @param from the namespace that owned the affected artifacts before the transfer, can't be {@literal null}.
		 * @param to the namespace that now owns the affected artifacts, can't be {@literal null}.
		 */
		public OwnershipTransferAccepted(EntityId id, @NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
			super(id, groupId, from, to);
		}
	}

	/**
	 * Event that would be published when an artifact ownership transfer request is rejected by the
	 * current owner. No artifact ownership is changed.
	 */
	@DomainEvent(name = "ownership-transfer.rejected", namespace = "artifactory")
	public static final class OwnershipTransferRejected extends OwnershipTransferEvent {

		/**
		 * Create a new {@link OwnershipTransferRejected} event for the given rejected transfer request.
		 *
		 * @param id the entity identifier of the rejected transfer request, can't be {@literal null}.
		 * @param groupId the artifact groupId coordinate whose transfer was rejected, can't be {@literal null}.
		 * @param from the namespace that owns the affected artifacts and rejected the request, can't be {@literal null}.
		 * @param to the namespace whose request to claim ownership was rejected, can't be {@literal null}.
		 */
		public OwnershipTransferRejected(EntityId id, @NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
			super(id, groupId, from, to);
		}
	}

	/**
	 * Event that would be published when an artifact ownership transfer request is cancelled by the
	 * requesting namespace. No artifact ownership is changed.
	 */
	@DomainEvent(name = "ownership-transfer.cancelled", namespace = "artifactory")
	public static final class OwnershipTransferCancelled extends OwnershipTransferEvent {

		/**
		 * Create a new {@link OwnershipTransferCancelled} event for the given cancelled transfer request.
		 *
		 * @param id the entity identifier of the cancelled transfer request, can't be {@literal null}.
		 * @param groupId the artifact groupId coordinate whose transfer was cancelled, can't be {@literal null}.
		 * @param from the namespace that owns the affected artifacts, can't be {@literal null}.
		 * @param to the namespace that cancelled its own request, can't be {@literal null}.
		 */
		public OwnershipTransferCancelled(EntityId id, @NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
			super(id, groupId, from, to);
		}
	}

}
