package com.konfigyr.vault.controller;

import com.konfigyr.vault.state.RepositoryStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VaultExceptionHandlerTest {

	@Mock
	MessageSource messageSource;

	WebRequest request;

	VaultExceptionHandler handler;

	@BeforeEach
	void setup() {
		request = new ServletWebRequest(new MockHttpServletRequest("GET", "/vault/state"));
		handler = new VaultExceptionHandler(messageSource);
	}

	@MethodSource("repositoryStateScenarios")
	@DisplayName("should handle repository state exception and create an error response")
	@ParameterizedTest(name = "should handle repository state exception with error code: {0}")
	void handleRepositoryStateException(RepositoryStateException.ErrorCode code, HttpStatus expectedStatusCode) {
		final var ex = new RepositoryStateException(code, "Repository state error message");
		final var response = handler.handleRepositoryStateException(request, ex);

		assertThat(response)
				.isNotNull()
				.returns(expectedStatusCode, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, ResponseEntity::getHeaders);

		final var body = response.getBody();

		assertThat(body)
				.isNotNull()
				.returns(expectedStatusCode.value(), ProblemDetail::getStatus)
				.returns(URI.create("http://localhost/vault/state"), ProblemDetail::getInstance)
				.returns(expectedStatusCode.getReasonPhrase(), ProblemDetail::getTitle)
				.returns("Repository state error message", ProblemDetail::getDetail);

		verify(messageSource).getMessage("problemDetail.type." + ex.getClass().getName() + "." + code,
				null, null, LocaleContextHolder.getLocale());
		verify(messageSource).getMessage("problemDetail.title." + ex.getClass().getName() + "." + code,
				null, null, LocaleContextHolder.getLocale());
		verify(messageSource).getMessage("problemDetail." + ex.getClass().getName() + "." + code,
				null, null, LocaleContextHolder.getLocale());
	}

	static Stream<Arguments> repositoryStateScenarios() {
		return Stream.of(
				Arguments.of(RepositoryStateException.ErrorCode.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
				Arguments.of(RepositoryStateException.ErrorCode.INVALID_STATE, HttpStatus.SERVICE_UNAVAILABLE),
				Arguments.of(RepositoryStateException.ErrorCode.CORRUPTED_STATE, HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of(RepositoryStateException.ErrorCode.CONFLICT, HttpStatus.CONFLICT),
				Arguments.of(RepositoryStateException.ErrorCode.INITIALIZATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, HttpStatus.NOT_FOUND),
				Arguments.of(RepositoryStateException.ErrorCode.REPOSITORY_ALREADY_EXISTS, HttpStatus.CONFLICT),
				Arguments.of(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, HttpStatus.NOT_FOUND),
				Arguments.of(RepositoryStateException.ErrorCode.PROFILE_ALREADY_EXISTS, HttpStatus.CONFLICT),
				Arguments.of(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, HttpStatus.NOT_FOUND),
				Arguments.of(RepositoryStateException.ErrorCode.UNKNOWN, HttpStatus.INTERNAL_SERVER_ERROR)
		);
	}
}
