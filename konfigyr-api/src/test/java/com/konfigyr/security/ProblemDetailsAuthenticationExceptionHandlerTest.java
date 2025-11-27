package com.konfigyr.security;

import com.konfigyr.test.assertions.ProblemDetailAssert;
import com.konfigyr.web.WebExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ProblemDetailsAuthenticationExceptionHandlerTest {

	static final ObjectMapper mapper = JsonMapper.builder()
			.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
			.build();

	@Mock
	WebExceptionHandler delegate;

	MockHttpServletRequest request;

	MockHttpServletResponse response;

	ProblemDetailsAuthenticationExceptionHandler handler;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		handler = new ProblemDetailsAuthenticationExceptionHandler(delegate);
	}

	@Test
	@DisplayName("should handle authentication exception")
	void shouldHandleAuthenticationException() {
		final var ex = new BadCredentialsException("Bad Credentials");
		final var problem = ErrorResponse.builder(ex, HttpStatus.UNAUTHORIZED, "Unauthorized")
				.build()
				.getBody();

		doReturn(ResponseEntity.of(problem).build()).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.commence(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(problem.getStatus());

		assertProblemDetails(response)
				.hasDefaultType()
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.hasTitle("Unauthorized")
				.hasDetail("Unauthorized");
	}

	@Test
	@DisplayName("should handle authentication exception without response body")
	void shouldHandleAuthenticationExceptionWithoutResponseBody() throws UnsupportedEncodingException {
		final var ex = new BadCredentialsException("Bad Credentials");

		doReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.commence(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(HttpStatus.UNAUTHORIZED.value());

		assertThat(response.getContentAsString())
				.isBlank();
	}

	@Test
	@DisplayName("should handle authentication exception with unsupported response body")
	void shouldHandleAuthenticationExceptionWithUnsupportedResponseBody() throws UnsupportedEncodingException {
		final var ex = new BadCredentialsException("Bad Credentials");

		doReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.commence(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(HttpStatus.UNAUTHORIZED.value());

		assertThat(response.getContentAsString())
				.isBlank();
	}

	@Test
	@DisplayName("should handle access denied exception")
	void shouldHandleAccessDeniedException() {
		final var ex = new AccessDeniedException("Access denied");
		final var problem = ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, "Denied")
				.build()
				.getBody();

		doReturn(ResponseEntity.of(problem).build()).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.handle(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(problem.getStatus());

		assertProblemDetails(response)
				.hasDefaultType()
				.hasStatus(HttpStatus.FORBIDDEN)
				.hasTitle("Forbidden")
				.hasDetail("Denied");
	}

	@Test
	@DisplayName("should handle access denied exception without response body")
	void shouldHandleAccessDeniedExceptionWithoutResponseBody() throws UnsupportedEncodingException {
		final var ex = new AccessDeniedException("Access denied");

		doReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build()).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.handle(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(HttpStatus.FORBIDDEN.value());

		assertThat(response.getContentAsString())
				.isBlank();
	}

	@Test
	@DisplayName("should handle access denied exception with unsupported response body")
	void shouldHandleAccessDeniedExceptionWithUnsupportedResponseBody() throws UnsupportedEncodingException {
		final var ex = new AccessDeniedException("Access denied");

		doReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Denied")).when(delegate).handle(request, response, ex);

		assertThatNoException().isThrownBy(() -> handler.handle(request, response, ex));

		assertThat(response.getStatus())
				.isEqualTo(HttpStatus.FORBIDDEN.value());

		assertThat(response.getContentAsString())
				.isBlank();
	}

	static ProblemDetailAssert assertProblemDetails(MockHttpServletResponse response) {
		return ProblemDetailAssert.assertThat(mapper.readValue(response.getContentAsByteArray(), ProblemDetail.class));
	}

}
