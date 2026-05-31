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
		permits KeysetManagementEvent.KeyEvent,
		KeysetManagementEvent.Created,
		KeysetManagementEvent.Rotated,
		KeysetManagementEvent.Deleted {

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
		 * Create a new {@link Rotated} event with the {@link EntityId entity identifiers} of the
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
	 * Event that would be published when {@link KeysetMetadata} is deleted from the system.
	 */
	@DomainEvent(name = "keyset-deleted", namespace = "kms")
	public static final class Deleted extends KeysetManagementEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifiers} of the
		 * {@link KeysetMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param id entity identifier of the keyset that was deleted
		 * @param namespace entity identifier of the namespace
		 */
		public Deleted(EntityId id, EntityId namespace) {
			super(id, namespace);
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} inside a {@link KeysetMetadata} changes
	 * state.
	 */
	public static abstract sealed class KeyEvent extends KeysetManagementEvent
			permits Deactivated, Reactivated, Compromised, Restored, Destroyed {

		private final String key;

		protected KeyEvent(KeyOperation operation, EntityId namespace) {
			this(operation.keyset(), operation.key(), namespace);
		}

		protected KeyEvent(EntityId keyset, String key, EntityId namespace) {
			super(keyset, namespace);
			this.key = key;
		}

		/**
		 * Returns the identifier of the {@link KeyMetadata} that was the subject of the operation.
		 *
		 * @return key metadata identifier, never {@literal null}
		 */
		@NonNull
		public String key() {
			return key;
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} within the {@link KeysetMetadata}
	 * is disabled or deactivated.
	 */
	@DomainEvent(name = "keyset-key-deactivated", namespace = "kms")
	public static final class Deactivated extends KeyEvent {

		/**
		 * Create a new {@link Deactivated} event with the {@link KeyOperation.DeactivateKey} operation
		 * and the {@link EntityId entity identifier} of the {@link Namespace} that is specified as
		 * the owner of the keyset.
		 *
		 * @param operation deactivate key operation
		 * @param namespace entity identifier of the namespace
		 */
		Deactivated(KeyOperation.DeactivateKey operation, EntityId namespace) {
			super(operation, namespace);
		}

		/**
		 * Create a new {@link Deactivated} event with the identifiers of the {@link KeysetMetadata},
		 * {@link KeyMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param keyset entity identifier of the keyset
		 * @param key entity identifier of the key that was deactivated
		 * @param namespace entity identifier of the namespace
		 */
		public Deactivated(EntityId keyset, String key, EntityId namespace) {
			super(keyset, key, namespace);
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} within the {@link KeysetMetadata}
	 * is activated again.
	 */
	@DomainEvent(name = "keyset-key-reactivated", namespace = "kms")
	public static final class Reactivated extends KeyEvent {

		/**
		 * Create a new {@link Deactivated} event with the {@link KeyOperation.DeactivateKey} operation
		 * and the {@link EntityId entity identifier} of the {@link Namespace} that is specified as
		 * the owner of the keyset.
		 *
		 * @param operation reactivate key operation
		 * @param namespace entity identifier of the namespace
		 */
		Reactivated(KeyOperation.ReactivateKey operation, EntityId namespace) {
			super(operation, namespace);
		}

		/**
		 * Create a new {@link Deactivated} event with the identifiers of the {@link KeysetMetadata},
		 * {@link KeyMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param keyset entity identifier of the keyset
		 * @param key entity identifier of the key that was deactivated
		 * @param namespace entity identifier of the namespace
		 */
		public Reactivated(EntityId keyset, String key, EntityId namespace) {
			super(keyset, key, namespace);
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} within the {@link KeysetMetadata}
	 * is compromised.
	 */
	@DomainEvent(name = "keyset-key-compromised", namespace = "kms")
	public static final class Compromised extends KeyEvent {

		/**
		 * Create a new {@link Deactivated} event with the {@link KeyOperation.DeactivateKey} operation
		 * and the {@link EntityId entity identifier} of the {@link Namespace} that is specified as
		 * the owner of the keyset.
		 *
		 * @param operation destroy key operation
		 * @param namespace entity identifier of the namespace
		 */
		Compromised(KeyOperation.CompromiseKey operation, EntityId namespace) {
			super(operation, namespace);
		}

		/**
		 * Create a new {@link Deleted} event with the identifiers of the {@link KeysetMetadata},
		 * {@link KeyMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param keyset entity identifier of the keyset
		 * @param key entity identifier of the key that is compromised
		 * @param namespace entity identifier of the namespace
		 */
		public Compromised(EntityId keyset, String key, EntityId namespace) {
			super(keyset, key, namespace);
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} within the {@link KeysetMetadata}
	 * was restored from the scheduled for destruction state.
	 */
	@DomainEvent(name = "keyset-key-restored", namespace = "kms")
	public static final class Restored extends KeyEvent {

		/**
		 * Create a new {@link Deactivated} event with the {@link KeyOperation.DeactivateKey} operation
		 * and the {@link EntityId entity identifier} of the {@link Namespace} that is specified as
		 * the owner of the keyset.
		 *
		 * @param operation restore key operation
		 * @param namespace entity identifier of the namespace
		 */
		Restored(KeyOperation.RestoreKey operation, EntityId namespace) {
			super(operation, namespace);
		}

		/**
		 * Create a new {@link Deactivated} event with the identifiers of the {@link KeysetMetadata},
		 * {@link KeyMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param keyset entity identifier of the keyset
		 * @param key entity identifier of the key that was restored
		 * @param namespace entity identifier of the namespace
		 */
		public Restored(EntityId keyset, String key, EntityId namespace) {
			super(keyset, key, namespace);
		}

	}

	/**
	 * Event that would be published when a {@link KeyMetadata} within the {@link KeysetMetadata}
	 * is scheduled for destruction.
	 */
	@DomainEvent(name = "keyset-key-destroyed", namespace = "kms")
	public static final class Destroyed extends KeyEvent {

		/**
		 * Create a new {@link Destroyed} event with the {@link KeyOperation.DeactivateKey} operation
		 * and the {@link EntityId entity identifier} of the {@link Namespace} that is specified as
		 * the owner of the keyset.
		 *
		 * @param operation destroy key operation
		 * @param namespace entity identifier of the namespace
		 */
		Destroyed(KeyOperation.DestroyKey operation, EntityId namespace) {
			super(operation, namespace);
		}

		/**
		 * Create a new {@link Destroyed} event with the identifiers of the {@link KeysetMetadata},
		 * {@link KeyMetadata} and the {@link Namespace} that is specified as the owner of the keyset.
		 *
		 * @param keyset entity identifier of the keyset
		 * @param key entity identifier of the key that should be destroyed
		 * @param namespace entity identifier of the namespace
		 */
		public Destroyed(EntityId keyset, String key, EntityId namespace) {
			super(keyset, key, namespace);
		}

	}

}
