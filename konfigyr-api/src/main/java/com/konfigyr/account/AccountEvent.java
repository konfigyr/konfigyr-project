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
public abstract sealed class AccountEvent extends EntityEvent permits AccountEvent.Registered {

	protected AccountEvent(EntityId id) {
		super(id);
	}

	/**
	 * Event that is published when the {@link Account} was successfully registered in our system.
	 */
	@DomainEvent(name = "registered", namespace = "accounts")
	public static final class Registered extends AccountEvent {

		/**
		 * Create a new {@link Registered} event with the {@link EntityId entity identifier} of the
		 * {@link Account} that was just created by the {@link AccountManager}.
		 *
		 * @param id entity identifier of the registered account
		 */
		public Registered(EntityId id) {
			super(id);
		}
	}

}
