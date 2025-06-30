package com.konfigyr.hateoas;

import com.konfigyr.security.access.AccessControlDecision;
import com.konfigyr.test.assertions.ProblemDetailAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HateoasExceptionHandlerTest {

	final HateoasExceptionHandler handler = create();

	MockHttpServletRequest request;
	MockHttpServletResponse response;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	@DisplayName("should handle non-mapped exceptions")
	void handleNonMappedExceptions() {
		expectProblemDetail(new IllegalArgumentException(), HttpStatus.INTERNAL_SERVER_ERROR)
				.hasTitle("Something went wrong")
				.hasDetail("An unexpected error occurred while processing your request. " +
						"Please try again later. If the issue persists, contact support.");

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handle non-mapped error response exceptions")
	void handleNonMappedErrorResponseExceptions() {
		final var ex = new NonMappedErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, "Outage");

		expectProblemDetail(ex, HttpStatus.SERVICE_UNAVAILABLE)
				.hasTitle(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
				.hasDetail("Outage");

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handle missing servlet request parameter exception")
	void missingServletRequestParameter() {
		final var ex = new MissingServletRequestParameterException("parameter", "string");

		expectProblemDetail(ex, HttpStatus.BAD_REQUEST)
				.isNotNull()
				.hasDefaultType()
				.hasTitle("Bad Request")
				.hasDetail("Required parameter 'parameter' is not present.")
				.hasProperty("errors", List.of(
						ValidationError.parameter("parameter", ex.getLocalizedMessage())
				));

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handle method argument invalid exception")
	void methodArgumentInvalid() {
		final var parameter = mock(MethodParameter.class);
		final var errors = new MapBindingResult(Map.of(), "map");
		errors.addError(new FieldError("map", "field_name", "required field"));
		errors.addError(new FieldError("map", "field_name", "invalid field"));

		final var ex = new MethodArgumentNotValidException(parameter, errors);

		expectProblemDetail(ex, HttpStatus.BAD_REQUEST)
				.hasDefaultType()
				.hasTitle("Invalid request content")
				.hasDetail("Your request contains invalid request data, please check the error response for more information.")
				.hasPropertySatisfying("errors", it -> assertThat(it)
						.asInstanceOf(InstanceOfAssertFactories.iterable(ValidationError.class))
						.containsExactlyInAnyOrder(
								ValidationError.pointer("field_name", "required field"),
								ValidationError.pointer("field_name", "invalid field")
						)
				);

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handler method validation exception")
	void handlerMethodValidationException() {
		final var ex = new HandlerMethodValidationException(createMethodValidationResult());

		expectProblemDetail(ex, HttpStatus.BAD_REQUEST)
				.hasDefaultType()
				.hasTitle("Invalid request content")
				.hasDetail("Your request contains invalid request data, please check the error response for more information.")
				.hasPropertySatisfying("errors", errors -> assertThat(errors)
						.asInstanceOf(InstanceOfAssertFactories.iterable(ValidationError.class))
						.containsExactlyInAnyOrder(
								ValidationError.header("header", "Missing request header"),
								ValidationError.parameter("request", "Missing request parameter"),
								ValidationError.pointer("body", "Invalid request body")
						)
				);

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should method validation exception")
	void methodValidationException() {
		final var ex = new MethodValidationException(createMethodValidationResult());

		expectProblemDetail(ex, HttpStatus.BAD_REQUEST)
				.hasDefaultType()
				.hasTitle("Invalid request content")
				.hasDetail("Your request contains invalid request data, please check the error response for more information.")
				.hasPropertySatisfying("errors", errors -> assertThat(errors)
						.asInstanceOf(InstanceOfAssertFactories.iterable(ValidationError.class))
						.containsExactlyInAnyOrder(
								ValidationError.header("header", "Missing request header"),
								ValidationError.parameter("request", "Missing request parameter"),
								ValidationError.pointer("body", "Invalid request body")
						)
				);

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@MethodSource("authorization")
	@DisplayName("should handle authorization denied exceptions")
	@ParameterizedTest(name = "should create error response for exception {0} with status code {1}")
	void shouldHandleAccessDeniedException(Class<? extends AccessDeniedException> type, HttpStatusCode status) {
		final var ex = mock(type);

		expectProblemDetail(ex, status)
				.hasDefaultType()
				.satisfies(it -> assertThat(it.getTitle())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.getDetail())
						.isNotBlank()
						.isNotEqualTo(ex.getMessage())
				);

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
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

		expectProblemDetail(new AuthorizationDeniedException("default error message", result), HttpStatus.FORBIDDEN)
				.hasDefaultType()
				.hasTitle("Access Denied")
				.hasDetailContaining("It looks like you do not have the necessary permissions to perform this operation");

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handle authorization denied exception with authorities decision")
	void shouldHandleAuthorityAuthorizationDecision() {
		final var result = new AuthorityAuthorizationDecision(false, AuthorityUtils.createAuthorityList("permission"));

		expectProblemDetail(new AuthorizationDeniedException("default error message", result), HttpStatus.FORBIDDEN)
				.hasDefaultType()
				.hasTitle("Access Denied")
				.hasDetailContaining("The following OAuth scopes are required to perform this operation: permission");

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@Test
	@DisplayName("should handle authorization denied exception with access control decision")
	void shouldHandleAccessControlDecision() {
		final var result = new AccessControlDecision(false, null, "permission");

		expectProblemDetail(new AuthorizationDeniedException("default error message", result), HttpStatus.FORBIDDEN)
				.hasDefaultType()
				.hasTitle("Access Denied")
				.hasDetailContaining("It looks like you do not have the necessary roles or permissions to perform this operation");

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
	}

	@MethodSource("authentication")
	@DisplayName("should handle authentication exceptions")
	@ParameterizedTest(name = "should create error response for exception {0} with status code {1}")
	void shouldHandleAuthenticationException(Class<? extends AuthenticationException> type, HttpStatusCode status) {
		final var ex = mock(type);

		expectProblemDetail(ex, status)
				.hasDefaultType()
				.satisfies(it -> assertThat(it.getTitle())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.getDetail())
						.isNotBlank()
						.isNotEqualTo(ex.getMessage())
				);

		assertThat(request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
				.isNull();
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

	ProblemDetailAssert expectProblemDetail(Exception ex, HttpStatusCode status) {
		System.out.println(
				handler.handle(request, response, ex)
		);
		return assertThat(handler.handle(request, response, ex))
				.isNotNull()
				.returns(status, ResponseEntity::getStatusCode)
				.returns(HttpHeaders.EMPTY, HttpEntity::getHeaders)
				.extracting(HttpEntity::getBody)
				.isInstanceOf(ProblemDetail.class)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasStatus(status);
	}

	static MethodValidationResult createMethodValidationResult() {
		final var result = mock(MethodValidationResult.class);

		doReturn(List.of(
				createParameterValidationResult("header", "Missing request header"),
				createParameterValidationResult("request", "Missing request parameter"),
				createParameterValidationResult("body", "Invalid request body")
		)).when(result).getParameterValidationResults();

		return result;
	}

	static ParameterValidationResult createParameterValidationResult(String name, String errorMessage) {
		final var parameter = mock(MethodParameter.class, withSettings().strictness(Strictness.LENIENT));
		doReturn(name).when(parameter).getParameterName();

		final var resolvable = new DefaultMessageSourceResolvable(new String[] {"error-code"}, errorMessage);

		if (name.equalsIgnoreCase("header")) {
			doReturn(true).when(parameter).hasParameterAnnotation(eq(RequestHeader.class));
		} else if (name.equalsIgnoreCase("request")) {
			doReturn(true).when(parameter).hasParameterAnnotation(eq(RequestParam.class));
		}

		final var result = mock(ParameterValidationResult.class);
		doReturn(parameter).when(result).getMethodParameter();
		doReturn(List.of(resolvable)).when(result).getResolvableErrors();

		return result;
	}

	static HateoasExceptionHandler create() {
		final var messages = new ResourceBundleMessageSource();
		messages.setBasenames("messages/problem-detail");

		final var handler = new HateoasExceptionHandler();
		handler.setMessageSource(messages);

		return handler;
	}

	static final class NonMappedErrorResponseException extends RuntimeException implements ErrorResponse {
		final ProblemDetail body;

		NonMappedErrorResponseException(HttpStatusCode status, String detail) {
			super(detail);
			body = ProblemDetail.forStatusAndDetail(status, detail);
		}

		@NonNull
		@Override
		public HttpStatusCode getStatusCode() {
			return HttpStatus.valueOf(body.getStatus());
		}

		@NonNull
		@Override
		public ProblemDetail getBody() {
			return body;
		}
	}

}
