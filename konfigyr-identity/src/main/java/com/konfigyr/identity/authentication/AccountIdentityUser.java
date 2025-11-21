package com.konfigyr.identity.authentication;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Interface that defines an {@link AuthenticatedPrincipal} in Spring Security context that is
 * based on the {@link AccountIdentity}.
 * <p>
 * There are two distinct implementations of this interface:
 * <ul>
 *     <li>{@link OidcAccountIdentityUser} - for OpenID Connect providers</li>
 *     <li>{@link OAuthAccountIdentityUser} - for OAuth2 providers</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public sealed interface AccountIdentityUser extends AuthenticatedPrincipal
		permits OAuthAccountIdentityUser, OidcAccountIdentityUser {

	/**
	 * Method that returns the {@link EntityId entity identifier} of the underlying {@link AccountIdentity}
	 * from this authenticated principal.
	 *
	 * @return the account identity entity identifier, never {@literal null}.
	 */
	@NonNull
	default EntityId getId() {
		return getAccountIdentity().getId();
	}

	/**
	 * Returns the name of the {@link AccountIdentityUser} that is in fact a serialized external value of the
	 * {@link EntityId entity identifier} of the {@link AccountIdentity}.
	 *
	 * @return the name of the authenticated principal, never {@literal null}.
	 */
	@NonNull
	@Override
	default String getName() {
		return getAccountIdentity().getName();
	}

	/**
	 * Get the underlying {@link AccountIdentity} from this authenticated principal.
	 *
	 * @return the account identity, never {@literal null}.
	 */
	@NonNull
	AccountIdentity getAccountIdentity();

}
