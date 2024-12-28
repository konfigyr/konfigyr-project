package com.konfigyr.identity.authorization;

import lombok.EqualsAndHashCode;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.time.Instant;

/**
 * Class that defines an event when an operation has been performed on the {@link OAuth2Authorization}
 * by the {@link AuthorizationService}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EqualsAndHashCode
public abstract sealed class AuthorizationEvent implements Serializable permits
		AuthorizationEvent.Stored, AuthorizationEvent.Revoked {

	/**
	 * The identifier of the {@link OAuth2Authorization} entity for which the event occurred.
	 */
	protected final String id;

	/**
	 * The {@link Instant timestamp} when the event occurred.
	 */
	protected final Instant timestamp;

	/**
	 * Create a new {@link AuthorizationEvent} with the {@link OAuth2Authorization} that is the
	 * subject of the event.
	 *
	 * @param authorization the authorisation for which the event is associated, never {@literal null}
	 */
	protected AuthorizationEvent(@NonNull OAuth2Authorization authorization) {
		this(authorization.getId());
	}

	/**
	 * Create a new {@link AuthorizationEvent} with the identifier of the {@link OAuth2Authorization}
	 * that is the subject of the event.
	 *
	 * @param id the authorisation identifier for which the event is associated, never {@literal null}
	 */
	protected AuthorizationEvent(String id) {
		Assert.hasText(id, "OAuth2 Authorization identifier cannot be empty");

		this.id = id;
		this.timestamp = Instant.now();
	}

	/**
	 * Returns identifier of the {@link OAuth2Authorization} for which the event occurred.
	 *
	 * @return authorization identifier, never {@literal null}
	 */
	@NonNull
	public String id() {
		return id;
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
		return "Authorization" + getClass().getSimpleName() + "[id=" + id + ", timestamp=" + timestamp() + ']';
	}

	/**
	 * Event that would be published when the {@link OAuth2Authorization} has been created or updated.
	 */
	@DomainEvent(name = "authorization-stored", namespace = "authorization")
	public static final class Stored extends AuthorizationEvent {

		/**
		 * Create a new {@link AuthorizationEvent.Stored} with the {@link OAuth2Authorization} that was stored
		 * by the {@link AuthorizationService}.
		 *
		 * @param authorization the stored authorisation, never {@literal null}
		 */
		public Stored(@NonNull OAuth2Authorization authorization) {
			super(authorization);
		}
	}

	/**
	 * Event that would be published when the {@link OAuth2Authorization} has been revoked.
	 */
	@DomainEvent(name = "authorization-revoked", namespace = "authorization")
	public static final class Revoked extends AuthorizationEvent {

		/**
		 * Create a new {@link AuthorizationEvent.Revoked} with the {@link OAuth2Authorization} that was removed
		 * or revoked by the {@link AuthorizationService}.
		 *
		 * @param authorization the revoked authorisation, never {@literal null}
		 */
		public Revoked(OAuth2Authorization authorization) {
			super(authorization);
		}

		/**
		 * Create a new {@link AuthorizationEvent.Revoked} with the identifier of the {@link OAuth2Authorization}
		 * that was removed or revoked by the {@link AuthorizationService}.
		 *
		 * @param id identifier of the revoked authorisation, never {@literal null}
		 */
		public Revoked(String id) {
			super(id);
		}
	}
}
