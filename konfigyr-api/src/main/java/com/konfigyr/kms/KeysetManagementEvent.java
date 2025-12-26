package com.konfigyr.kms;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;

/**
 * Abstract event type that should be used for all {@link KeysetManager Keyset Management} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class KeysetManagementEvent
		extends EntityEvent
		permits KeysetManagementEvent.Created,
		KeysetManagementEvent.Rotated,
		KeysetManagementEvent.Disabled,
		KeysetManagementEvent.Activated,
		KeysetManagementEvent.Removed,
		KeysetManagementEvent.Destroyed {

	private final EntityId namespace;

	protected KeysetManagementEvent(@NonNull EntityId id, @NonNull EntityId namespace) {
		super(id);
		this.namespace = namespace;
	}

	/**
	 * Returns the {@link EntityId} of the {@link Namespace} associated with the {@link KeysetMetadata}
	 * for which the event occurred.
	 *
	 * @return namespace entity identifier, never {@literal null}
	 */
	@NonNull
	public EntityId namespace() {
		return namespace;
	}

	/**
	 * Event that would be published when a new {@link KeysetMetadata} is created.
	 */
	@DomainEvent(name = "keyset-created", namespace = "kms")
	public static final class Created extends KeysetManagementEvent {

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the created keyset
		 * @param namespace entity identifier of the namespace
		 */
		public Created(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when {@link KeysetMetadata} is rotated.
	 */
	@DomainEvent(name = "keyset-rotated", namespace = "kms")
	public static final class Rotated extends KeysetManagementEvent {

		/**
		 * Create a new {@link Removed} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the keyset that was rotated
		 * @param namespace entity identifier of the namespace
		 */
		public Rotated(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when {@link KeysetMetadata} is disabled.
	 */
	@DomainEvent(name = "keyset-disabled", namespace = "kms")
	public static final class Disabled extends KeysetManagementEvent {

		/**
		 * Create a new {@link Disabled} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the disabled keyset
		 * @param namespace entity identifier of the namespace
		 */
		public Disabled(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when {@link KeysetMetadata} is activated.
	 */
	@DomainEvent(name = "keyset-activated", namespace = "kms")
	public static final class Activated extends KeysetManagementEvent {

		/**
		 * Create a new {@link Disabled} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the activated keyset
		 * @param namespace entity identifier of the namespace
		 */
		public Activated(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when {@link KeysetMetadata} is scheduled for removal.
	 */
	@DomainEvent(name = "keyset-disabled", namespace = "kms")
	public static final class Removed extends KeysetManagementEvent {

		/**
		 * Create a new {@link Removed} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the keyset scheduled for removal
		 * @param namespace entity identifier of the namespace
		 */
		public Removed(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when {@link KeysetMetadata} is destroyed.
	 */
	@DomainEvent(name = "keyset-disabled", namespace = "kms")
	public static final class Destroyed extends KeysetManagementEvent {

		/**
		 * Create a new {@link Removed} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the keyset that was removed
		 * @param namespace entity identifier of the namespace
		 */
		public Destroyed(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

}
