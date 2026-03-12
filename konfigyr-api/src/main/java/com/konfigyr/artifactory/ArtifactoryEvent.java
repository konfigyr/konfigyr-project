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
public abstract sealed class ArtifactoryEvent extends EntityEvent permits ArtifactoryEvent.ArtifactEvent {

	protected ArtifactoryEvent(EntityId id) {
		super(id);
	}

	/**
	 * Abstract event type used for events related to a {@link VersionedArtifact}.
	 */
	public static sealed abstract class ArtifactEvent extends ArtifactoryEvent
			permits ReleaseCreated, ReleaseCompleted, ReleaseFailed {

		private final ArtifactCoordinates coordinates;

		/**
		 * Create a new {@link ArtifactEvent} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact artifact version to create the event for, can't be {@literal null}.
		 */
		protected ArtifactEvent(@NonNull VersionedArtifact artifact) {
			this(artifact.id(), artifact.coordinates());
		}

		/**
		 * Create a new {@link ArtifactEvent} event with the {@link EntityId entity identifier}
		 * of the {@link VersionedArtifact} and it's coordinates.
		 *
		 * @param id entity identifier of the created artifact version, can't be {@literal null}.
		 * @param coordinates the artifact coordinates of the created artifact version, can't be {@literal null}.
		 */
		protected ArtifactEvent(EntityId id, @NonNull ArtifactCoordinates coordinates) {
			super(id);
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
	@DomainEvent(name = "artifact-version.release-created", namespace = "artifactory")
	public static final class ReleaseCreated extends ArtifactEvent {

		/**
		 * Create a new {@link ReleaseCreated} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was just created by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the created artifact version
		 * @param coordinates the artifact coordinates of the created artifact version
		 */
		public ReleaseCreated(EntityId id, ArtifactCoordinates coordinates) {
			super(id, coordinates);
		}
	}

	/**
	 * Event that would be published when a new {@link VersionedArtifact} is successfully released.
	 */
	@DomainEvent(name = "artifact-version.release-completed", namespace = "artifactory")
	public static final class ReleaseCompleted extends ArtifactEvent {

		/**
		 * Create a new {@link ReleaseCompleted} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact affected artifact version, can't be {@literal null}.
		 */
		public ReleaseCompleted(VersionedArtifact artifact) {
			this(artifact.id(), artifact.coordinates());
		}

		/**
		 * Create a new {@link ReleaseCompleted} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was successfully released by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the artifact version
		 * @param coordinates the artifact coordinates of the artifact version
		 */
		public ReleaseCompleted(EntityId id, ArtifactCoordinates coordinates) {
			super(id, coordinates);
		}
	}

	/**
	 * Event that would be published when a release for {@link VersionedArtifact} failed.
	 */
	@DomainEvent(name = "artifact-version.release-failed", namespace = "artifactory")
	public static final class ReleaseFailed extends ArtifactEvent {

		/**
		 * Create a new {@link ReleaseFailed} event for the {@link VersionedArtifact}.
		 *
		 * @param artifact affected artifact version, can't be {@literal null}.
		 */
		public ReleaseFailed(VersionedArtifact artifact) {
			this(artifact.id(), artifact.coordinates());
		}

		/**
		 * Create a new {@link ReleaseFailed} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was could not be released by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the artifact version
		 * @param coordinates the artifact coordinates of the artifact version
		 */
		public ReleaseFailed(EntityId id, ArtifactCoordinates coordinates) {
			super(id, coordinates);
		}
	}

}
