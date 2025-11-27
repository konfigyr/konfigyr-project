package com.konfigyr.identity.authentication;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import java.io.Serial;

/**
 * Exception thrown when creating an {@link AccountIdentity} from the {@link OAuth2AuthenticatedPrincipal}
 * with an email address that already exists in the system.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Getter
public class AccountIdentityExistsException extends OAuth2AuthenticationException {

	@Serial
	private static final long serialVersionUID = -7619499327519601936L;

	/**
	 * The {@link OAuth2AuthenticatedPrincipal} that was extracted from the OAuth2 or OpenID Authorization server.
	 */
	private final OAuth2AuthenticatedPrincipal user;

	/**
	 * Create new instance for the {@link OAuth2AuthenticatedPrincipal} that triggered the unique constraint
	 * violation when creating an {@link AccountIdentity}.
	 *
	 * @param user OAuth2 user data that triggered this exception, can't be {@literal null}
	 */
	public AccountIdentityExistsException(@NonNull OAuth2AuthenticatedPrincipal user) {
		super(new OAuth2Error(
				OAuth2ErrorCodes.SERVER_ERROR,
				"Could not register account as one already exists with that email address",
				null
		));
		this.user = user;
	}

}
