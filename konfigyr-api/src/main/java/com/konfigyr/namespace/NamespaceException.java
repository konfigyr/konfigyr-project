package com.konfigyr.namespace;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@link Namespace namespaces}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -5579152178839019368L;

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public NamespaceException(String message) {
		super(message);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public NamespaceException(String message, Throwable cause) {
		super(message, cause);
	}
}
