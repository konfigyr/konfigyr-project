package com.konfigyr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.web.WebExceptionHandler;
import com.konfigyr.web.converter.ProblemDetailHttpMessageConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@RequiredArgsConstructor
final class ProblemDetailsAuthenticationExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HttpMessageConverter<Object> httpMessageConverter;
	private final WebExceptionHandler delegate;

	public ProblemDetailsAuthenticationExceptionHandler(WebExceptionHandler delegate) {
		this(new ProblemDetailHttpMessageConverter(), delegate);
	}

	public ProblemDetailsAuthenticationExceptionHandler(ObjectMapper mapper, WebExceptionHandler delegate) {
		this(new ProblemDetailHttpMessageConverter(mapper), delegate);
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
		write(request, response, ex);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
		write(request, response, ex);
	}

	private void write(HttpServletRequest request, HttpServletResponse response, Exception cause) throws IOException {
		final ResponseEntity<Object> result = delegate.handle(request, response, cause);

		try (final ServletServerHttpResponse output = new ServletServerHttpResponse(response)) {
			output.setStatusCode(result.getStatusCode());
			output.getHeaders().putAll(result.getHeaders());

			if (result.getBody() != null) {
				httpMessageConverter.write(result.getBody(), MediaType.APPLICATION_PROBLEM_JSON, output);
			}
		}
	}
}
