package com.konfigyr.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CsrfRequestMatcherTest {

	@Mock
	BearerTokenResolver resolver;

	@Mock
	RequestMatcher delegate;

	HttpServletRequest request;
	RequestMatcher matcher;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		matcher = new CsrfRequestMatcher(resolver, delegate);
	}

	@Test
	@DisplayName("should not invoke delegate request matcher when bearer token is present")
	void tokenPresent() {
		doReturn("access-token").when(resolver).resolve(request);

		assertThat(matcher.matches(request)).isFalse();

		verifyNoInteractions(delegate);
	}

	@Test
	@DisplayName("should invoke delegate request matcher when bearer token is missing")
	void tokenMissing() {
		doReturn(true).when(delegate).matches(request);

		assertThat(matcher.matches(request)).isTrue();

		verify(delegate).matches(request);
	}

	@Test
	@DisplayName("should invoke delegate request matcher when bearer token can not be resolved")
	void resolverFails() {
		doThrow(OAuth2AuthenticationException.class).when(resolver).resolve(request);
		doReturn(false).when(delegate).matches(request);

		assertThat(matcher.matches(request)).isFalse();

		verify(delegate).matches(request);
	}

}
