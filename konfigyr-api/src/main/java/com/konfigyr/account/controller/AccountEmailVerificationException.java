package com.konfigyr.account.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.ErrorResponse;

import java.io.Serial;

@Getter
class AccountEmailVerificationException extends NestedRuntimeException implements ErrorResponse {

	@Serial
	private static final long serialVersionUID = 149490472901502070L;

	private final ErrorCode code;
	private final ProblemDetail body;

	AccountEmailVerificationException(ErrorCode code, String message) {
		super(message);
		this.code = code;
		this.body = ProblemDetail.forStatusAndDetail(code.status, message);
	}

	AccountEmailVerificationException(ErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
	}

	@NonNull
	@Override
	public HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}

	@NonNull
	@Override
	public HttpStatusCode getStatusCode() {
		return code.status;
	}

	@NonNull
	@Override
	public String getDetailMessageCode() {
		return "problemDetail." + getClass().getName() + "." + code.name();
	}

	@NonNull
	@Override
	public String getTitleMessageCode() {
		return "problemDetail.title." + getClass().getName() + "." + code.name();
	}

	@RequiredArgsConstructor
	enum ErrorCode {
		EMAIL_UNAVAILABLE(HttpStatus.BAD_REQUEST),
		INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST),
		JWT_ENCODER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
		JWT_DECODER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
		MAILER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

		final HttpStatus status;
	}

}
