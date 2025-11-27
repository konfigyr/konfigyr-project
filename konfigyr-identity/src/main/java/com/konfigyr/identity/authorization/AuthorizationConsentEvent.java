
package com.konfigyr.identity.authorization;

import lombok.EqualsAndHashCode;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
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
public abstract sealed class AuthorizationConsentEvent implements Serializable permits
		AuthorizationConsentEvent.Granted, AuthorizationConsentEvent.Revoked {

	/**
	 * The {@link OAuth2AuthorizationConsent} entity for which the event occurred.
	 */
	protected final OAuth2AuthorizationConsent consent;

	/**
	 * The {@link Instant timestamp} when the event occurred.
	 */
	protected final Instant timestamp;

	/**
	 * Create a new {@link AuthorizationConsentEvent} with the {@link OAuth2AuthorizationConsent} that is the
	 * subject of the event.
	 *
	 * @param consent the authorisation consent for which the event is associated, never {@literal null}
	 */
	protected AuthorizationConsentEvent(OAuth2AuthorizationConsent consent) {
		Assert.notNull(consent, "OAuth2 Authorization consent cannot be null");

		this.consent = consent;
		this.timestamp = Instant.now();
	}

	/**
	 * Returns the {@link OAuth2AuthorizationConsent} for which the event occurred.
	 *
	 * @return authorization consent, never {@literal null}
	 */
	@NonNull
	public OAuth2AuthorizationConsent consent() {
		return consent;
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
		return "AuthorizationConsent" + getClass().getSimpleName() + "[timestamp=" + timestamp() + ']';
	}

	/**
	 * Event that would be published when the {@link OAuth2AuthorizationConsent} has been granted.
	 */
	@DomainEvent(name = "authorization-consent-granted", namespace = "authorization")
	public static final class Granted extends AuthorizationConsentEvent {

		/**
		 * Create a new {@link Granted} with the {@link OAuth2AuthorizationConsent} that was granted
		 * by the {@link AuthorizationService}.
		 *
		 * @param consent the stored authorisation consent, never {@literal null}
		 */
		public Granted(@NonNull OAuth2AuthorizationConsent consent) {
			super(consent);
		}
	}

	/**
	 * Event that would be published when the {@link OAuth2AuthorizationConsent} has been revoked.
	 */
	@DomainEvent(name = "authorization-consent-revoked", namespace = "authorization")
	public static final class Revoked extends AuthorizationConsentEvent {

		/**
		 * Create a new {@link Revoked} with the {@link OAuth2AuthorizationConsent} that was removed
		 * or revoked by the {@link AuthorizationService}.
		 *
		 * @param consent the revoked authorisation consent, never {@literal null}
		 */
		public Revoked(OAuth2AuthorizationConsent consent) {
			super(consent);
		}

	}
}
