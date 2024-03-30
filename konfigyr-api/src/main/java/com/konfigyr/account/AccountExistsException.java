package com.konfigyr.account;

import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception thrown during {@link AccountRegistration account registration} where there is already
 * an {@link Account} with the same email address.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class AccountExistsException extends AccountException {
	@Serial
	private static final long serialVersionUID = -7619499327519601936L;

	/**
	 * The {@link AccountRegistration} that was used when this exception was thrown.
	 */
	@NonNull
	private final AccountRegistration registration;

	/**
	 * Create new instance for the {@link AccountRegistration} that trigger the unique constraint
	 * violation when creating an {@link Account}.
	 *
	 * @param registration registration data that triggered this exception
	 * @param cause the actual cause that triggered this exception
	 */
	public AccountExistsException(@NonNull AccountRegistration registration, Throwable cause) {
		super("Could not register account as one already exists with that email address", cause);
		this.registration = registration;
	}

	/**
	 * Returns the {@link AccountRegistration} instance that was used when this exception was thrown.
	 * @return account registration, never {@literal null}
	 */
	@NonNull
	public AccountRegistration getRegistration() {
		return registration;
	}
}
