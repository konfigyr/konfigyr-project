package com.konfigyr.account;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;

/**
 * Abstract event type that should be used for all {@link Account} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class AccountEvent extends EntityEvent
		permits AccountEvent.Updated, AccountEvent.Deleted {

	protected AccountEvent(EntityId id) {
		super(id);
	}

	/**
	 * Event that is published when the {@link Account} was successfully updated in our system.
	 */
	@DomainEvent(name = "updated", namespace = "accounts")
	public static final class Updated extends AccountEvent {

		/**
		 * Create a new {@link Updated} event with the {@link EntityId entity identifier} of the
		 * {@link Account} that was just updated by the {@link AccountManager}.
		 *
		 * @param id entity identifier of the updated account
		 */
		public Updated(EntityId id) {
			super(id);
		}
	}

	/**
	 * Event that is published when the {@link Account} was successfully removed from our system.
	 */
	@DomainEvent(name = "deleted", namespace = "accounts")
	public static final class Deleted extends AccountEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifier} of the
		 * {@link Account} that was just removed by the {@link AccountManager}.
		 *
		 * @param id entity identifier of the deleted account
		 */
		public Deleted(EntityId id) {
			super(id);
		}
	}

}
