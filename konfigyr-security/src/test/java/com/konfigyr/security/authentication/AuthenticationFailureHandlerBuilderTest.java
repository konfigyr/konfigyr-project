package com.konfigyr.security.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AuthenticationFailureHandlerBuilderTest {

	MockHttpServletRequest request;
	MockHttpServletResponse response;
	AuthenticationFailureHandlerBuilder builder;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		builder = new AuthenticationFailureHandlerBuilder("/error");
	}

	@Test
	@DisplayName("should create simple URL authentication failure handler")
	void shouldCreateSimpleHandler() {
		final var handler = builder.build();

		assertThat(handler)
				.isInstanceOf(SimpleUrlAuthenticationFailureHandler.class);

		assertThatNoException().isThrownBy(
				() -> handler.onAuthenticationFailure(request, response, new BadCredentialsException("It's bad man"))
		);

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/error");

		assertThat(request.getSession())
				.isNotNull()
				.extracting(it -> it.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	@DisplayName("should create delegating authentication failure handler with failure URL")
	void shouldCreateDelegatingHandlerWithFailureUrl() {
		final var handler = builder
				.register(BadCredentialsException.class, "/bad-credentials")
				.build();

		assertThat(handler)
				.isInstanceOf(DelegatingAuthenticationFailureHandler.class);

		assertThatNoException().isThrownBy(
				() -> handler.onAuthenticationFailure(request, response, new BadCredentialsException("It's bad man"))
		);

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/bad-credentials");
	}

	@Test
	@DisplayName("should create delegating authentication failure handler with custom handler")
	void shouldCreateDelegatingHandler() throws Exception {
		final var delegate = mock(AuthenticationFailureHandler.class);

		final var handler = builder
				.register(BadCredentialsException.class, delegate)
				.build();

		assertThat(handler)
				.isInstanceOf(DelegatingAuthenticationFailureHandler.class);

		assertThatNoException().isThrownBy(
				() -> handler.onAuthenticationFailure(request, response, new BadCredentialsException("It's bad man"))
		);

		assertThatNoException().isThrownBy(
				() -> handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("It's expired man"))
		);

		verify(delegate).onAuthenticationFailure(eq(request), eq(response), any(BadCredentialsException.class));

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/error");
	}

	@Test
	@DisplayName("should check if authentication failure handler is being overwritten")
	void shouldCheckForOverrides() {
		final var delegate = mock(AuthenticationFailureHandler.class);

		assertThatNoException().isThrownBy(() -> builder.register(BadCredentialsException.class, delegate));
		assertThatNoException().isThrownBy(() -> builder.register(CredentialsExpiredException.class, delegate));
		assertThatThrownBy(() -> builder.register(CredentialsExpiredException.class, delegate))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already exists. You attempted to override it with");
	}

	@Test
	@DisplayName("should check if authentication failure handler URL is supplied")
	void shouldValidateFailureUrl() {
		assertThatThrownBy(() -> new AuthenticationFailureHandlerBuilder(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Failure URL can not be blank");

		assertThatThrownBy(() -> new AuthenticationFailureHandlerBuilder(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Failure URL can not be blank");

		assertThatThrownBy(() -> new AuthenticationFailureHandlerBuilder("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Failure URL can not be blank");

		assertThatThrownBy(() -> builder.register(CredentialsExpiredException.class, ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Failure redirect URL can not be blank");

		assertThatThrownBy(() -> builder.register(CredentialsExpiredException.class, " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Failure redirect URL can not be blank");
	}

}