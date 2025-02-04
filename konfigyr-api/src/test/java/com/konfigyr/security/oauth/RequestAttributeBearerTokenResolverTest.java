package com.konfigyr.security.oauth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestAttributeBearerTokenResolverTest {

	@Mock
	BearerTokenResolver delegate;

	HttpServletRequest request;
	BearerTokenResolver resolver;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		resolver = new RequestAttributeBearerTokenResolver(delegate);
	}

	@Test
	@DisplayName("should not invoke delegate resolver when bearer token is present")
	void attributePresent() {
		request.setAttribute(RequestAttributeBearerTokenResolver.REQUEST_ATTRIBUTE_NAME, "access-token");

		assertThat(resolver.resolve(request))
				.isNotBlank()
				.isEqualTo("access-token");

		verifyNoInteractions(delegate);
	}

	@Test
	@DisplayName("should resolve bearer token using delegate and store it as request attribute")
	void attributeMissing() {
		doReturn("access-token").when(delegate).resolve(request);

		assertThat(resolver.resolve(request))
				.isNotBlank()
				.isEqualTo("access-token");

		assertThat(request.getAttribute(RequestAttributeBearerTokenResolver.REQUEST_ATTRIBUTE_NAME))
				.isEqualTo("access-token");

		verify(delegate).resolve(request);
	}

	@Test
	@DisplayName("should fail to resolve bearer token using delegate")
	void bearerTokenMissing() {
		assertThat(resolver.resolve(request))
				.isNull();

		assertThat(request.getAttribute(RequestAttributeBearerTokenResolver.REQUEST_ATTRIBUTE_NAME))
				.isNull();

		verify(delegate).resolve(request);
	}

	@Test
	@DisplayName("should not catch any exceptions from delegate resolver")
	void rethrowExceptions() {
		doThrow(OAuth2AuthenticationException.class).when(delegate).resolve(request);

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> resolver.resolve(request));

		assertThat(request.getAttribute(RequestAttributeBearerTokenResolver.REQUEST_ATTRIBUTE_NAME))
				.isNull();

		verify(delegate).resolve(request);
	}

}
