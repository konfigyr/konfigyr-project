package com.konfigyr.kms.controller;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeysetOperation;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Locale;

@NullMarked
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice("com.konfigyr.kms.controller")
class CryptoExceptionHandler {

	private final MessageSource messageSource;

	@ExceptionHandler(CryptoException.KeysetOperationException.class)
	ResponseEntity<ProblemDetail> handleKeysetOperationException(CryptoException.KeysetOperationException ex) {
		final KeysetOperation operation = ex.attemptedOperation();
		final String key = operation.name().toLowerCase(Locale.ROOT);

		return createResponse(
				ErrorResponse.builder(ex, HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage())
						.titleMessageCode("problemDetail.title.com.konfigyr.crypto.operation-error.%s".formatted(key))
						.detailMessageCode("problemDetail.com.konfigyr.crypto.operation-error.%s".formatted(key))
						.detailMessageArguments(operation, ex.getName())
						.property("operation", operation)
						.build()
		);
	}

	@ExceptionHandler(CryptoException.UnsupportedKeysetOperationException.class)
	ResponseEntity<ProblemDetail> handleUnsupportedKeysetOperationException(CryptoException.UnsupportedKeysetOperationException ex) {
		final List<String> supported = ex.supportedOperations().stream()
				.map(Enum::name)
				.sorted()
				.toList();

		return createResponse(
				ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage())
						.titleMessageCode("problemDetail.title.com.konfigyr.crypto.unsupported-keyset-operation")
						.detailMessageCode("problemDetail.com.konfigyr.crypto.unsupported-keyset-operation")
						.detailMessageArguments(ex.attemptedOperation(), String.join(", ", supported), ex.getName())
						.property("operation", ex.attemptedOperation())
						.property("supported", supported)
						.build()
		);
	}

	private ResponseEntity<ProblemDetail> createResponse(ErrorResponse error) {
		final ProblemDetail body = error.updateAndGetBody(messageSource, LocaleContextHolder.getLocale());

		return new ResponseEntity<>(body, error.getHeaders(), error.getStatusCode());
	}
}
