package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ControllerAdvice
public class HateoasExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	final ResponseEntity<Object> handleAuthenticationException(@NonNull AuthenticationException ex, @NonNull WebRequest request) {
		final HttpHeaders headers = new HttpHeaders();

		return switch (ex) {
			case InternalAuthenticationServiceException exception -> handleInternalServerException(
					exception, headers, request
			);
			case ProviderNotFoundException exception -> handleInternalServerException(
					exception, headers, request
			);
			case InvalidCookieException exception -> handleInternalServerException(
					exception, headers, request
			);
			case InsufficientAuthenticationException exception -> handleException(
					exception, HttpStatus.FORBIDDEN, headers, request
			);
			default -> handleException(
					ex, HttpStatus.UNAUTHORIZED, headers, request
			);
		};
	}

	@ExceptionHandler(AccessDeniedException.class)
	final ResponseEntity<Object> handleAccessDeniedException(@NonNull AccessDeniedException ex, @NonNull WebRequest request) {
		if (ex instanceof AuthorizationDeniedException exception) {
			return handleAuthorizationDeniedException(exception, request);
		}

		final ProblemDetail body = createProblemDetail(ex, HttpStatus.FORBIDDEN, ex.getMessage(), null, null, request);
		return createResponseEntity(body, HttpHeaders.EMPTY, HttpStatus.FORBIDDEN, request);
	}

	protected ResponseEntity<Object> handleAuthorizationDeniedException(
			@NonNull AuthorizationDeniedException ex, @NonNull WebRequest request
	) {
		final AuthorizationResult result = ex.getAuthorizationResult();

		final ErrorResponse response = switch (result) {
			case AuthorityAuthorizationDecision decision -> ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, ex.getMessage())
					.detailMessageCode(ErrorResponse.getDefaultDetailMessageCode(result.getClass(), null))
					.detailMessageArguments(authoritiesToString(decision.getAuthorities()))
					.build(getMessageSource(), LocaleContextHolder.getLocale());

			default -> ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, ex.getMessage())
					.build(getMessageSource(), LocaleContextHolder.getLocale());
		};

		return createResponseEntity(response.getBody(), HttpHeaders.EMPTY, HttpStatus.FORBIDDEN, request);
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			@NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request
	) {
		final ProblemDetail body = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
		body.setProperty("errors", List.of(Error.parameter(
				ex.getParameterName(),
				ex.getLocalizedMessage()
		)));

		return createResponseEntity(body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request
	) {
		final Locale locale = LocaleContextHolder.getLocale();
		final List<Error> errors = new ArrayList<>();

		for (FieldError error : ex.getFieldErrors()) {
			errors.add(Error.pointer(error.getField(), messageFor(error, locale)));
		}

		final ProblemDetail body = ex.updateAndGetBody(getMessageSource(), locale);
		body.setProperty("errors", errors);

		return createResponseEntity(body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			@NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatusCode status, @NonNull WebRequest request
	) {
		return handleMethodValidationResult(ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale()),
				ex.getParameterValidationResults(), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodValidationException(
			@NonNull MethodValidationException ex, @NonNull HttpHeaders headers,
			@NonNull HttpStatus status, @NonNull WebRequest request
	) {
		final ProblemDetail body = createProblemDetail(ex, status, "Validation failed", null, null, request);
		return handleMethodValidationResult(body, ex.getParameterValidationResults(), headers, status, request);
	}

	protected ResponseEntity<Object> handleMethodValidationResult(
			@NonNull ProblemDetail body, @NonNull Iterable<ParameterValidationResult> results,
			@NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request
	) {
		final Locale locale = LocaleContextHolder.getLocale();
		final List<Error> errors = new ArrayList<>();

		for (ParameterValidationResult result : results) {
			final MethodParameter parameter = result.getMethodParameter();

			for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
				final Error error;

				if (parameter.hasParameterAnnotation(RequestHeader.class)) {
					error = Error.header(parameter.getParameterName(), messageFor(resolvable, locale));
				} else if (parameter.hasParameterAnnotation(RequestParam.class)) {
					error = Error.parameter(parameter.getParameterName(), messageFor(resolvable, locale));
				} else {
					error = Error.pointer(parameter.getParameterName(), messageFor(resolvable, locale));
				}

				errors.add(error);
			}
		}

		body.setProperty("errors", errors);
		return createResponseEntity(body, headers, status, request);
	}

	protected ResponseEntity<Object> handleInternalServerException(
			@NonNull Exception ex, @NonNull HttpHeaders headers, @NonNull WebRequest request
	) {
		return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, headers, request);
	}

	protected ResponseEntity<Object> handleException(
			@NonNull Exception ex, @NonNull HttpStatusCode statusCode,
			@NonNull HttpHeaders headers, @NonNull WebRequest request
	) {
		final ProblemDetail body = createProblemDetail(ex, statusCode, ex.getMessage(), null, null, request);
		return createResponseEntity(body, headers, statusCode, request);
	}

	private String messageFor(@NonNull MessageSourceResolvable resolvable, @NonNull Locale locale) {
		if (getMessageSource() == null) {
			return resolvable.getDefaultMessage();
		}
		return getMessageSource().getMessage(resolvable, locale);
	}

	protected static String authoritiesToString(Collection<? extends GrantedAuthority> authorities) {
		return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Error(
			@NonNull String detail,
			@Nullable String pointer,
			@Nullable String parameter,
			@Nullable String header
	) {
		static Error pointer(String name, String detail) {
			return new Error(detail, name, null, null);
		}

		static Error parameter(String name, String detail) {
			return new Error(detail, null, name, null);
		}

		static Error header(String name, String detail) {
			return new Error(detail, null, null, name);
		}
	}
}
