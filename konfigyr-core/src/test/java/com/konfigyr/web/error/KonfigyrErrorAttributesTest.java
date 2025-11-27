package com.konfigyr.web.error;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class KonfigyrErrorAttributesTest {

	final ErrorAttributes attributes = new KonfigyrErrorAttributes();

	WebRequest request;

	@BeforeEach
	void setup() {
		request = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	@DisplayName("should retrieve authentication exception")
	void shouldRetrieveAuthenticationException() {
		final var cause = new BadCredentialsException("Bad credentials");

		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, cause, RequestAttributes.SCOPE_REQUEST);

		assertThat(attributes.getError(request))
				.isNotNull()
				.isEqualTo(cause);
	}

	@Test
	@DisplayName("should retrieve exception using default implementation")
	void shouldRetrieveExceptionUsingDefaultImplementation() {
		final var cause = new IllegalArgumentException("Bad argument");

		request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, cause, RequestAttributes.SCOPE_REQUEST);
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION,
				new BadCredentialsException("Bad credentials"), RequestAttributes.SCOPE_REQUEST);

		assertThat(attributes.getError(request))
				.isNotNull()
				.isEqualTo(cause);
	}

	@Test
	@DisplayName("should return null when no exception is present")
	void shouldReturnNullWhenNoExceptionIsPresent() {
		assertThat(attributes.getError(request))
				.isNull();
	}

}
