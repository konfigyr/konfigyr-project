package com.konfigyr.identity.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.net.URI;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationFailureHandlerTest {

	MockHttpServletRequest request;
	MockHttpServletResponse response;

	final AuthorizationFailureHandler handler = new AuthorizationFailureHandler();

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	@DisplayName("should forward to OAuth error page for non OAuth authentication exceptions")
	void forwardForNonOAuthExceptions() {
		handle(new BadCredentialsException("Bad credentials"));

		assertThat(response.getForwardedUrl())
				.isEqualTo("/oauth/error");

		assertThat(response.getRedirectedUrl())
				.isNull();
	}

	@Test
	@DisplayName("should forward to OAuth error page when redirect_uri is not set")
	void forwardWhenRedirectUriIsNotSet() {
		final var token = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
		final var ex = new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error("error_code"), token
		);

		handle(ex);

		assertThat(response.getForwardedUrl())
				.isEqualTo("/oauth/error");

		assertThat(response.getRedirectedUrl())
				.isNull();

		verify(token).getRedirectUri();
	}

	@Test
	@DisplayName("should forward to OAuth error page when OAuth error is not found")
	void forwardWhenOAuthErrorIsNotFound() {
		final var token = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
		final var ex = mock(OAuth2AuthorizationCodeRequestAuthenticationException.class);

		doReturn("/oauth/client").when(token).getRedirectUri();
		doReturn(token).when(ex).getAuthorizationCodeRequestAuthentication();

		handle(ex);

		assertThat(response.getForwardedUrl())
				.isEqualTo("/oauth/error");

		assertThat(response.getRedirectedUrl())
				.isNull();
	}

	@Test
	@DisplayName("should redirect to redirect_uri with error code only")
	void forwardRedirectWithErrorCodeOnly() {
		final var token = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
		doReturn("/oauth/client").when(token).getRedirectUri();

		final var ex = new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error("error_code"), token
		);

		handle(ex);

		assertThat(response.getForwardedUrl())
				.isNull();

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/oauth/client?error=error_code");
	}

	@Test
	@DisplayName("should redirect to redirect_uri with error details")
	void forwardRedirectWithErrorDetails() {
		final var token = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
		doReturn("/oauth/client").when(token).getRedirectUri();
		doReturn("state").when(token).getState();

		final var ex = new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error("error_code", "error description", "error uri"),
				token
		);

		handle(ex);

		assertThat(response.getForwardedUrl())
				.isNull();

		assertThat(response.getRedirectedUrl())
				.isNotNull();

		assertThat(URI.create(response.getRedirectedUrl()))
				.hasPath("/oauth/client")
				.hasParameter("error", "error_code")
				.hasParameter("error_description", "error description")
				.hasParameter("error_uri", "error uri")
				.hasParameter("state", "state");
	}

	private void handle(AuthenticationException exception) {
		assertThatNoException().isThrownBy(() -> handler.onAuthenticationFailure(request, response, exception));
	}

}
