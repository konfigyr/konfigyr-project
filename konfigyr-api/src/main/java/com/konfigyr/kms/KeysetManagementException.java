package com.konfigyr.kms;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.io.Serial;

/**
 * Exception type that would usually be thrown when interacting with {@link KeysetManager}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public class KeysetManagementException extends NestedRuntimeException implements ErrorResponse {

	@Serial
	private static final long serialVersionUID = 6522674567410911158L;

	private final HttpStatusCode statusCode;
	private final ProblemDetail body;

	public KeysetManagementException(String msg) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, msg);
	}

	public KeysetManagementException(HttpStatusCode statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.body = ProblemDetail.forStatusAndDetail(statusCode, message);
	}

	public KeysetManagementException(String message, @Nullable Throwable cause) {
		this(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
	}

	public KeysetManagementException(HttpStatusCode statusCode, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
		this.body = ProblemDetail.forStatusAndDetail(statusCode, message);
	}

	@Override
	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	@Override
	public ProblemDetail getBody() {
		return body;
	}
}
