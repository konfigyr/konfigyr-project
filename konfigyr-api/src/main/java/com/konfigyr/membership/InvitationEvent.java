package com.konfigyr.membership;

import com.konfigyr.account.Account;
import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Abstract event type for all {@link Invitation}-related lifecycle events.
 * <p>
 * Each subtype corresponds to a distinct transition in the {@link InvitationState} lifecycle.
 * These events are published by the {@link Invitations} service and may be consumed by other
 * components such as email senders, audit listeners, or integration adapters.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class InvitationEvent extends EntityEvent implements Supplier<Namespace>
		permits InvitationEvent.Created, InvitationEvent.Accepted, InvitationEvent.Declined, InvitationEvent.Canceled {

	protected final String key;
	protected final Namespace namespace;

	protected InvitationEvent(Namespace namespace, String key) {
		super(namespace.id());
		Assert.hasText(key, "Invitation key must not be empty");
		this.key = key;
		this.namespace = namespace;
	}

	/**
	 * Returns the {@link Namespace} for which the invitation event occurred.
	 *
	 * @return the namespace, never {@literal null}.
	 */
	@Override
	public Namespace get() {
		return namespace;
	}

	/**
	 * Returns the {@link EntityId} of the {@link Namespace} associated with the {@link Invitation}
	 * for which the event occurred.
	 *
	 * @return namespace entity identifier, never {@literal null}
	 */
	@NonNull
	public EntityId namespace() {
		return id;
	}

	/**
	 * Returns the key of the {@link Invitation} for which the event occurred.
	 *
	 * @return invitation key, never {@literal null}
	 */
	@NonNull
	public String key() {
		return key;
	}

	/**
	 * Event published when a new {@link Invitation} is created and sent to the recipient.
	 */
	@DomainEvent(name = "invitation-created", namespace = "namespace")
	public static final class Created extends InvitationEvent {

		/**
		 * Create a new {@link Created} event for the given {@link Namespace} and invitation key.
		 *
		 * @param namespace the namespace for which the invitation was created
		 * @param key       invitation key
		 */
		public Created(Namespace namespace, String key) {
			super(namespace, key);
		}
	}

	/**
	 * Event published when a recipient accepts an {@link Invitation} and becomes a namespace member.
	 */
	@DomainEvent(name = "invitation-accepted", namespace = "namespace")
	public static final class Accepted extends InvitationEvent {

		private final Account recipient;

		/**
		 * Create a new {@link Accepted} event for the given {@link Namespace}, recipient, and invitation key.
		 *
		 * @param namespace the namespace the recipient is joining
		 * @param recipient the account that accepted the invitation
		 * @param key       invitation key
		 */
		public Accepted(Namespace namespace, Account recipient, String key) {
			super(namespace, key);
			this.recipient = recipient;
		}

		/**
		 * Returns the {@link Account} that accepted the invitation.
		 *
		 * @return the recipient account, never {@literal null}
		 */
		public Account recipient() {
			return recipient;
		}
	}

	/**
	 * Event published when a recipient explicitly declines an {@link Invitation}.
	 */
	@DomainEvent(name = "invitation-declined", namespace = "namespace")
	public static final class Declined extends InvitationEvent {

		private final Account recipient;

		/**
		 * Create a new {@link Declined} event for the given {@link Namespace}, recipient, and invitation key.
		 *
		 * @param namespace the namespace for which the invitation was declined
		 * @param recipient the account that declined the invitation
		 * @param key       invitation key
		 */
		public Declined(Namespace namespace, Account recipient, String key) {
			super(namespace, key);
			this.recipient = recipient;
		}

		/**
		 * Returns the {@link Account} that declined the invitation.
		 *
		 * @return the recipient account, never {@literal null}
		 */
		public Account recipient() {
			return recipient;
		}
	}

	/**
	 * Event published when a namespace administrator cancels an {@link Invitation} before the
	 * recipient has acted on it.
	 */
	@DomainEvent(name = "invitation-canceled", namespace = "namespace")
	public static final class Canceled extends InvitationEvent {

		/**
		 * Create a new {@link Canceled} event for the given {@link Namespace} and invitation key.
		 *
		 * @param namespace the namespace for which the invitation was canceled
		 * @param key       invitation key
		 */
		public Canceled(Namespace namespace, String key) {
			super(namespace, key);
		}
	}

}
