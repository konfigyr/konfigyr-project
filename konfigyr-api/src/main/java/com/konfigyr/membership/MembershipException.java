package com.konfigyr.membership;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@link Invitations} or {@link Memberships}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Getter
public class MembershipException extends NestedRuntimeException implements ErrorResponse {

	@Serial
	private static final long serialVersionUID = 3608929360545001766L;

	protected final HttpStatusCode statusCode;
	protected final ProblemDetail body;

	/**
	 * Create a new instance of {@link MembershipException} with the specified error message.
	 *
	 * @param message the error message
	 */
	public MembershipException(String message) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

	/**
	 * Create a new instance of {@link MembershipException} with the specified error message.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 */
	public MembershipException(HttpStatusCode statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.body = ProblemDetail.forStatusAndDetail(statusCode, message);
	}

	/**
	 * Create a new instance of {@link MembershipException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public MembershipException(String message, Throwable cause) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
	}

	/**
	 * Create a new instance of {@link MembershipException} with the specified error message and
	 * the exception that caused it.
	 *
	 * @param statusCode HTTP status code
	 * @param message the error message
	 * @param cause the exception cause, or {@literal null} if none
	 */
	public MembershipException(HttpStatusCode statusCode, String message, Throwable cause) {
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
