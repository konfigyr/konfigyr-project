package com.konfigyr.security.provision;

import com.konfigyr.test.TestPrincipals;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

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
	SecurityContextHolderStrategy contextHolderStrategy;

	@BeforeEach
	void setup() throws Exception {
		contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		filter = new ProvisioningRedirectFilter();
		filter.setProvisioningUrl("/account-provisioning-url");
		filter.setContextHolderStrategy(contextHolderStrategy);
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

		assertThatThrownBy(() -> filter.setContextHolderStrategy(null))
				.describedAs("Should validate context holder strategy")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should not redirect when requesting provisioning page")
	void shouldNotRedirectForTargetPage() throws Exception {
		request.setPathInfo("/account-provisioning-url");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when no authentication is present")
	void shouldNotRedirectWhenAuthenticationIsMissing() throws Exception {
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when authentication is containing an account principal")
	void shouldNotFilterForAccountPrincipal() throws Exception {
		setupPrincipal(TestPrincipals.john());
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should not redirect when authentication is anonymous")
	void shouldNotFilterForAnonymous() throws Exception {
		setupPrincipal(new AnonymousAuthenticationToken("anon", "anon", AuthorityUtils.createAuthorityList("anon")));
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redirectStrategy);

		assertThat(response.getRedirectedUrl()).isNullOrEmpty();
	}

	@Test
	@DisplayName("should redirect when authentication is not containing the account principal")
	void shouldNotRedirectForUnsupportedAuthenticationPrincipal() throws Exception {
		setupPrincipal(new TestingAuthenticationToken("user", "pass", AuthorityUtils.NO_AUTHORITIES));
		request.setPathInfo("/some-page");

		filter.doFilter(request, response, chain);

		verifyNoInteractions(chain);
		verify(redirectStrategy).sendRedirect(request, response, "/account-provisioning-url");

		assertThat(response.getRedirectedUrl())
				.isEqualTo("/account-provisioning-url");
	}

	void setupPrincipal(Authentication authentication) {
		final var context = contextHolderStrategy.getContext();
		context.setAuthentication(authentication);
	}

}