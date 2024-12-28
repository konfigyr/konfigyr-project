package com.konfigyr.identity.authentication;

/**
 * Enumeration that defines the current status of the {@link AccountIdentity}.
 *
 * @author Vladimir Spasic
 * @since 1.0.
 **/
public enum AccountIdentityStatus {

	/**
	 * Accounts have an active status when they are successfully provisioned after their registration.
	 */
	ACTIVE,

	/**
	 * Accounts have a suspended status when administrator explicitly suspends them or the user has
	 * reached the maximum number of log-in attempts.
	 */
	SUSPENDED,

	/**
	 * Accounts would be in a deactivated state when they are removed from the system.
	 */
	DEACTIVATED
}
