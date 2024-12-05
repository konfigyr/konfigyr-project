package com.konfigyr.security.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.WebAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class LoginControllerErrorExtractorTest {

	MockHttpServletRequest request;
	MockHttpSession session;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		session = new MockHttpSession();

		request.setSession(session);
	}

	@Test
	@DisplayName("should resolve to null when no session or request attributes are present")
	void shouldResolveNoOAuthError() {
		assertThat(LoginSecurityController.extractError(request))
				.isNull();
	}

	@Test
	@DisplayName("should resolve to null when exception is of different type")
	void shouldResolveNoOAuthErrorWhenNotOAuthException() {
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Wrong..."));
		session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Wrong..."));

		assertThat(LoginSecurityController.extractError(request))
				.isNull();
	}

	@Test
	@DisplayName("should resolve OAuth error from request attribute")
	void shouldResolveOAuthErrorFromRequestAttribute() {
		final var ex = new OAuth2AuthenticationException(OAuth2ErrorCodes.INSUFFICIENT_SCOPE);
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, ex);

		assertThat(LoginSecurityController.extractError(request))
				.isEqualTo(ex.getError());
	}

	@Test
	@DisplayName("should resolve OAuth error from session attribute")
	void shouldResolveOAuthErrorFromSessionAttribute() {
		final var error = new OAuth2Error(OAuth2ErrorCodes.INSUFFICIENT_SCOPE);
		session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new OAuth2AuthorizationException(error));

		assertThat(LoginSecurityController.extractError(request))
				.isEqualTo(error);
	}

}
