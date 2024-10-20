package com.konfigyr.security.provision;

import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.io.Serial;

/**
 * Authentication exception thrown when the {@link com.konfigyr.security.AccountPrincipal} does not
 * exist in our system. Usually this is when the users are authenticated via OAuth Client login flow.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProvisioningHints
 **/
public class ProvisioningRequiredException extends AuthenticationException {

	@Serial
	private static final long serialVersionUID = -5940678897311858094L;

	private final ProvisioningHints hints;

	public ProvisioningRequiredException() {
		this(ProvisioningHints.EMPTY);
	}

	public ProvisioningRequiredException(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull String provider) {
		this(ProvisioningHints.from(user, provider));
	}

	public ProvisioningRequiredException(@NonNull ProvisioningHints hints) {
		super("Account provisioning is required to successfully authenticate the principal");

		this.hints = hints;
	}

	/**
	 * Get the provisioning hints that can be used to create an account.
	 *
	 * @return provisioning hints, never {@code null}
	 */
	@NonNull
	public ProvisioningHints getHints() {
		return this.hints;
	}
}
