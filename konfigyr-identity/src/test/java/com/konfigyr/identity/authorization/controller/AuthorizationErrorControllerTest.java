package com.konfigyr.identity.authorization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class AuthorizationErrorControllerTest {

	final MockMvcTester mvc = MockMvcTester.of(new AuthorizationErrorController());

	@Test
	@DisplayName("should render OAuth error page with internal server error")
	void shouldRenderInternalServerError() {
		mvc.get().uri("/oauth/error")
				.accept(MediaType.TEXT_HTML)
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasViewName("oauth-error")
				.model()
				.hasEntrySatisfying("error", it -> assertThat(it)
						.isInstanceOf(OAuth2Error.class)
						.asInstanceOf(type(OAuth2Error.class))
						.returns(OAuth2ErrorCodes.SERVER_ERROR, OAuth2Error::getErrorCode)
						.returns(null, OAuth2Error::getDescription)
						.returns(null, OAuth2Error::getUri)
				);
	}

	@Test
	@DisplayName("should render OAuth error page with OAuth error")
	void shouldRenderOAuthError() {
		final var error = new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "Client problem", null);

		mvc.get().uri("/oauth/error")
				.accept(MediaType.TEXT_HTML)
				.requestAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new OAuth2AuthenticationException(error))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasViewName("oauth-error")
				.model()
				.containsEntry("error", error);
	}

	@Test
	@DisplayName("should render OAuth error as JSON response")
	void shouldRenderJSON() {
		final var error = new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
				"Client problem", "https://oauth.com/problem-uri");

		mvc.get().uri("/oauth/error")
				.requestAttr(WebAttributes.AUTHENTICATION_EXCEPTION, new OAuth2AuthenticationException(error))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.hasPathSatisfying("$.error", it -> assertThat(it)
						.isEqualTo(error.getErrorCode())
				)
				.hasPathSatisfying("$.error_description", it -> assertThat(it)
						.isEqualTo(error.getDescription())
				)
				.hasPathSatisfying("$.error_uri", it -> assertThat(it)
						.isEqualTo(error.getUri())
				);
	}

}
