package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Abstract event type that should be used for all {@link Invitation} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class InvitationEvent extends EntityEvent permits
		InvitationEvent.Created, InvitationEvent.Accepted {

	protected final String key;

	protected InvitationEvent(EntityId id, String key) {
		super(id);
		Assert.hasText(key, "Invitation key must not be empty");
		this.key = key;
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
	 * Returns the {@link Invitation} key of the {@link Invitation} for which the event occurred.
	 *
	 * @return entity identifier, never {@literal null}
	 */
	@NonNull
	public String key() {
		return key;
	}

	/**
	 * Event that would be published when a new {@link Invitation} is created.
	 */
	@DomainEvent(name = "invitation-created", namespace = "namespace")
	public static final class Created extends InvitationEvent {

		private final UriComponents host;

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} and the {@link Invitation} key that was just created by the {@link Invitations}.
		 *
		 * @param id entity identifier of the created invitation
		 * @param key invitation key
		 */
		public Created(EntityId id, String key, UriComponents host) {
			super(id, key);
			this.host = host;
		}

		/**
		 * Returns the {@link UriComponents} host instance that can be used to create email links.
		 *
		 * @return the host used to construct links, never {@literal null}
		 */
		@NonNull
		public UriComponents host() {
			return host;
		}
	}

	/**
	 * Event that would be published when a new {@link Invitation} is accepted.
	 */
	@DomainEvent(name = "invitation-accepted", namespace = "namespace")
	public static final class Accepted extends InvitationEvent {

		/**
		 * Create a new {@link Accepted} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} and the {@link Invitation} key that was just accepted by the {@link Invitations}.
		 *
		 * @param id entity identifier of the created namespace
		 * @param key invitation key
		 */
		public Accepted(EntityId id, String key) {
			super(id, key);
		}
	}

}
