package com.konfigyr.identity.authentication;

import lombok.EqualsAndHashCode;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.time.Instant;

/**
 * Class that defines an event when an operation has been performed on the {@link AccountIdentity}
 * by the {@link AccountIdentityService}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EqualsAndHashCode
public abstract sealed class AccountIdentityEvent implements Serializable {

	/**
	 * The {@link AccountIdentity} for which the event occurred.
	 */
	protected final AccountIdentity identity;

	/**
	 * The {@link Instant timestamp} when the event occurred.
	 */
	protected final Instant timestamp;

	public AccountIdentityEvent(AccountIdentity identity) {
		Assert.notNull(identity, "Account identity cannot be null");

		this.identity = identity;
		this.timestamp = Instant.now();
	}

	/**
	 * Returns {@link AccountIdentity} for which the event occurred.
	 *
	 * @return account identity, never {@literal null}
	 */
	@NonNull
	public AccountIdentity identity() {
		return identity;
	}

	/**
	 * Returns the {@link Instant} when the event occurred.
	 *
	 * @return event timestamp, never {@literal null}
	 */
	@NonNull
	public Instant timestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "AccountIdentity" + getClass().getSimpleName() +
				"[identity=" + identity + ", timestamp=" + timestamp() + ']';
	}

	/**
	 * Event that would be published when the {@link AccountIdentity} has been created.
	 *
	 * @author Vladimir Spasic
	 * @since 1.0.0
	 */
	@DomainEvent(name = "account-identity-created", namespace = "authentication")
	public static final class Created extends AccountIdentityEvent {

		/**
		 * Create a new {@link AccountIdentityEvent.Created} with the {@link AccountIdentity} that was created
		 * by the {@link AccountIdentityService}.
		 *
		 * @param identity the created account identity, never {@literal null}
		 */
		public Created(AccountIdentity identity) {
			super(identity);
		}
	}
}
