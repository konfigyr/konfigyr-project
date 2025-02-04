package com.konfigyr.namespace;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@link Namespace namespaces}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceException extends ErrorResponseException {

	@Serial
	private static final long serialVersionUID = -5579152178839019368L;

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public NamespaceException(String message) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 */
	public NamespaceException(HttpStatusCode statusCode, String message) {
		this(statusCode, message, null);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified {@link ProblemDetail}.
	 *
	 * @param body the problem detail body
	 */
	public NamespaceException(ProblemDetail body) {
		super(HttpStatus.valueOf(body.getStatus()), body, null);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public NamespaceException(String message, Throwable cause) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public NamespaceException(HttpStatusCode statusCode, String message, Throwable cause) {
		super(statusCode, ProblemDetail.forStatusAndDetail(statusCode, message), cause);
	}

	/**
	 * Create a new instance of {@link NamespaceException} with the specified {@link ProblemDetail} and
	 * the exception that caused it.
	 *
	 * @param body the problem detail body
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public NamespaceException(ProblemDetail body, Throwable cause) {
		super(HttpStatus.valueOf(body.getStatus()), body, cause);
	}
}
