package com.konfigyr.security.provision;

import com.konfigyr.security.provisioning.ProvisioningRequiredException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProvisioningRedirectFilterTest {

	@Mock
	FilterChain chain;

	@Spy
	RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	ProvisioningRedirectFilter filter;
	MockHttpServletRequest request;
	MockHttpServletResponse response;

	@BeforeEach
	void setup() throws Exception {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		filter = new ProvisioningRedirectFilter();
		filter.setProvisioningUrl("/account-provisioning-url");
		filter.setRedirectStrategy(redirectStrategy);
		filter.afterPropertiesSet();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("should validate setters")
	void shouldValidateSetters() {
		assertThatThrownBy(() -> filter.setProvisioningUrl(null))
				.describedAs("Should validate provisioning URL")
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> filter.setProvisioningUrl(""))
				.describedAs("Should validate provisioning URL")
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> filter.setProvisioningUrl(" "))
				.describedAs("Should validate provisioning URL")
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> filter.setRedirectStrategy(null))
				.describedAs("Should validate redirect strategy")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should not redirect when requesting provisioning page")
	void shouldNotRedirectForTargetPage() throws Exception {
		setupSession(new ProvisioningRequiredException());
		request.setPathInfo("/account-provisioning-url");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when no http session is present")
	void shouldNotRedirectWhenSessionIsMissing() throws Exception {
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when exception is not in the session")
	void shouldNotRedirectWhenExceptionIsMissing() throws Exception {
		setupSession(null);
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when exception is not a provisioning required one")
	void shouldNotRedirectWhenOtherExceptionIsPresent() throws Exception {
		setupSession(new AuthenticationCredentialsNotFoundException("Nope, not there"));
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should redirect when provisioning required exception is in session")
	void shouldRedirectWhenProvisioningIsRequired() throws Exception {
		setupSession(new ProvisioningRequiredException());
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verifyNoInteractions(chain);
		verify(redirectStrategy).sendRedirect(request, response, "/account-provisioning-url");

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/account-provisioning-url");
	}

	@MethodSource("patterns")
	@DisplayName("should evaluate request matcher patterns")
	@ParameterizedTest(name = "[provisioningUrl={0}, requestUri={1}, shouldNotFilter={2}]")
	void evaluateRequestMatcherPatterns(String redirectUrl, String requestUri, boolean result) {
		filter.setProvisioningUrl(redirectUrl);
		request.setPathInfo(requestUri);

		assertThat(filter.shouldNotFilter(request)).isEqualTo(result);
	}

	static Stream<Arguments> patterns() {
		return Stream.of(
				Arguments.of("/account-provisioning-url", "/account-provisioning-url", true),
				Arguments.of("/account-provisioning-url/", "/account-provisioning-url", true),
				Arguments.of("/account-provisioning-url", "/account-provisioning-url/", true),
				Arguments.of("/account-provisioning-url/", "/account-provisioning-url/", true),
				Arguments.of("/account-provisioning-url", "/account-provisioning-url/path", true),
				Arguments.of("/account-provisioning-url/", "/account-provisioning-url/path", true),
				Arguments.of("/account-provisioning-url", "/account-provisioning-url/path/sub", true),
				Arguments.of("/account-provisioning-url", "/account-provisioning", false),
				Arguments.of("/account/provision/", "/account/provision", true),
				Arguments.of("/account/provision", "/account/provision/do", true),
				Arguments.of("/account/provision", "/account/provisioning", false),
				Arguments.of("/account/provision", "/account-provision", false),
				Arguments.of("/account/provision", "/login", false),
				Arguments.of("/account/provision", "/", false)
		);
	}


	void setupSession(AuthenticationException exception) {
		final var session = new MockHttpSession();
		session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);

		request.setSession(session);
	}

}