package com.konfigyr.artifactory;

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.ErrorResponse;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@code Artifactory} Domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ArtifactoryException extends NestedRuntimeException implements ErrorResponse {

	@Serial
	private static final long serialVersionUID = 8276087255913011097L;

	private final HttpStatusCode statusCode;
	private final ProblemDetail body;

	/**
	 * Create a new instance of {@link ArtifactoryException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public ArtifactoryException(String message) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

	/**
	 * Create a new instance of {@link ArtifactoryException} with the specified error message and HTTP status code.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 */
	public ArtifactoryException(HttpStatusCode statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.body = ProblemDetail.forStatusAndDetail(statusCode, message);
	}

	/**
	 * Create a new instance of {@link ArtifactoryException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public ArtifactoryException(String message, Throwable cause) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
	}

	/**
	 * Create a new instance of {@link ArtifactoryException} with the specified error message, HTTP status code
	 * and the exception that caused it.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public ArtifactoryException(HttpStatusCode statusCode, String message, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
		this.body = ProblemDetail.forStatusAndDetail(statusCode, message);
	}

	@NonNull
	@Override
	public HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}

	@NonNull
	@Override
	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	@NonNull
	@Override
	public ProblemDetail getBody() {
		return body;
	}

}
