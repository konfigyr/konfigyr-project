package com.konfigyr.account;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@link Account accounts}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class AccountException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -7619499327519601936L;

	/**
	 * Create a new instance of {@link AccountException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public AccountException(String message) {
		super(message);
	}

	/**
	 * Create a new instance of {@link AccountException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public AccountException(String message, Throwable cause) {
		super(message, cause);
	}
}
