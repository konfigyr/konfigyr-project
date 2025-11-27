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
public abstract sealed class ArtifactoryEvent extends EntityEvent permits ArtifactoryEvent.Release {

	protected ArtifactoryEvent(EntityId id) {
		super(id);
	}

	/**
	 * Event that would be published when a new {@link VersionedArtifact} is created.
	 */
	@DomainEvent(name = "artifact-version.release", namespace = "artifactory")
	public static final class Release extends ArtifactoryEvent {

		private final ArtifactCoordinates coordinates;

		/**
		 * Create a new {@link Release} event with the {@link EntityId entity identifier} of the
		 * {@link VersionedArtifact} that was just created by the {@link Artifactory}.
		 *
		 * @param id entity identifier of the created artifact version
		 * @param coordinates the artifact coordinates of the created artifact version
		 */
		public Release(EntityId id, ArtifactCoordinates coordinates) {
			super(id);
			this.coordinates = coordinates;
		}

		/**
		 * Returns the {@link ArtifactCoordinates} of the released {@link VersionedArtifact}.
		 *
		 * @return artifact version coordinates, never {@literal null}.
		 */
		@NonNull
		public ArtifactCoordinates coordinates() {
			return coordinates;
		}
	}

}
