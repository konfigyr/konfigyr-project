package com.konfigyr.security.access;

import org.springframework.core.NestedRuntimeException;

import java.io.Serial;

/**
 * Abstract base class for {@link AccessControl} operations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public abstract class AccessControlException extends NestedRuntimeException {

	@Serial
	private static final long serialVersionUID = 6179810569647384554L;

	/**
	 * Constructs an {@link AccessControlException} with the specified message without root cause.
	 *
	 * @param msg the detail message
	 */
	public AccessControlException(String msg) {
		super(msg);
	}

	/**
	 * Constructs an {@link AccessControlException} with the specified message and root cause.
	 *
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public AccessControlException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
