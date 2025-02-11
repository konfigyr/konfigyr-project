package com.konfigyr.hateoas;

import com.konfigyr.security.access.AccessControlDecision;
import com.konfigyr.test.assertions.ProblemDetailAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.*;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.ExpressionAuthorizationDecision;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HateoasExceptionHandlerTest {

	final HateoasExceptionHandler handler = create();

	@Mock
	WebRequest request;

	@MethodSource("authorization")
	@DisplayName("should handle authorization denied exceptions")
	@ParameterizedTest(name = "should create error response for exception {0} with status code {1}")
	void shouldHandleAccessDeniedException(Class<? extends AccessDeniedException> type, HttpStatusCode status) {
		final var ex = mock(type);
		doReturn("default error message").when(ex).getMessage();

		assertThat(handler.handleAccessDeniedException(ex, request))
				.isNotNull()
				.returns(status, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasStatus(status)
				.hasDetailContaining("default error message");
	}

	static Stream<Arguments> authorization() {
		return Stream.of(
				Arguments.of(AccessDeniedException.class, HttpStatus.FORBIDDEN),
				Arguments.of(CsrfException.class, HttpStatus.FORBIDDEN),
				Arguments.of(InvalidCsrfTokenException.class, HttpStatus.FORBIDDEN),
				Arguments.of(MissingCsrfTokenException.class, HttpStatus.FORBIDDEN),
				Arguments.of(AuthorizationServiceException.class, HttpStatus.INTERNAL_SERVER_ERROR)
		);
	}

	@Test
	@DisplayName("should handle authorization denied exception with expression decision")
	void shouldHandleExpressionAuthorizationDecision() {
		final var result = new ExpressionAuthorizationDecision(false, null);
		final var ex = new AuthorizationDeniedException("default error message", result);

		assertThat(handler.handleAccessDeniedException(ex, request))
				.isNotNull()
				.returns(HttpStatus.FORBIDDEN, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasStatus(HttpStatus.FORBIDDEN)
				.hasTitle("Access Denied")
				.hasDetailContaining("It looks like you do not have the necessary permissions to perform this operation");
	}

	@Test
	@DisplayName("should handle authorization denied exception with authorities decision")
	void shouldHandleAuthorityAuthorizationDecision() {
		final var result = new AuthorityAuthorizationDecision(false, AuthorityUtils.createAuthorityList("permission"));
		final var ex = new AuthorizationDeniedException("default error message", result);

		assertThat(handler.handleAccessDeniedException(ex, request))
				.isNotNull()
				.returns(HttpStatus.FORBIDDEN, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasStatus(HttpStatus.FORBIDDEN)
				.hasTitle("Access Denied")
				.hasDetailContaining("The following OAuth scopes are required to perform this operation: permission");
	}

	@Test
	@DisplayName("should handle authorization denied exception with access control decision")
	void shouldHandleAccessControlDecision() {
		final var result = new AccessControlDecision(false, null, "permission");
		final var ex = new AuthorizationDeniedException("default error message", result);

		assertThat(handler.handleAccessDeniedException(ex, request))
				.isNotNull()
				.returns(HttpStatus.FORBIDDEN, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasStatus(HttpStatus.FORBIDDEN)
				.hasTitle("Access Denied")
				.hasDetailContaining("It looks like you do not have the necessary roles or permissions to perform this operation");
	}

	@MethodSource("authentication")
	@DisplayName("should handle authentication exceptions")
	@ParameterizedTest(name = "should create error response for exception {0} with status code {1}")
	void shouldHandleAuthenticationException(Class<? extends AuthenticationException> type, HttpStatusCode status) {
		final var ex = mock(type);
		doReturn("default error message").when(ex).getMessage();

		assertThat(handler.handleAuthenticationException(ex, request))
				.isNotNull()
				.returns(status, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasStatus(status)
				.hasDetailContaining("default error message");
	}

	static Stream<Arguments> authentication() {
		return Stream.of(
				Arguments.of(InternalAuthenticationServiceException.class, HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of(ProviderNotFoundException.class, HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of(InvalidCookieException.class, HttpStatus.INTERNAL_SERVER_ERROR),
				Arguments.of(InsufficientAuthenticationException.class, HttpStatus.FORBIDDEN),
				Arguments.of(AuthenticationCredentialsNotFoundException.class, HttpStatus.UNAUTHORIZED),
				Arguments.of(BadCredentialsException.class, HttpStatus.UNAUTHORIZED)
		);
	}

	static HateoasExceptionHandler create() {
		final var messages = new ResourceBundleMessageSource();
		messages.setBasenames("messages/problem-detail");

		final var handler = new HateoasExceptionHandler();
		handler.setMessageSource(messages);

		return handler;
	}

}
