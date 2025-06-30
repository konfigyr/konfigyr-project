package com.konfigyr.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Interface that handles all Spring MVC raised exceptions by returning a {@link ResponseEntity}, usually
 * with the {@link org.springframework.http.ProblemDetail RFC 9457} formatted error details in the body.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface WebExceptionHandler {

	/**
	 * Handles the exception that was raised within handling of the request.
	 *
	 * @param request the current request, can't be {@literal null}
	 * @param response the current response, can't be {@literal null}
	 * @param ex the exception to handle, can't be {@literal null}
	 * @return HTTP response entity containing the handled exception body, never {@literal null}
	 */
	@NonNull
	default ResponseEntity<Object> handle(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull Exception ex
	) {
		return handle(new ServletWebRequest(request, response), ex);
	}

	/**
	 * Handles the exception that was raised within handling of the request.
	 *
	 * @param request the current web request, can't be {@literal null}
	 * @param ex the exception to handle, can't be {@literal null}
	 * @return HTTP response entity containing the handled exception body, never {@literal null}
	 */
	@NonNull
	ResponseEntity<Object> handle(@NonNull WebRequest request, @NonNull Exception ex);

}
