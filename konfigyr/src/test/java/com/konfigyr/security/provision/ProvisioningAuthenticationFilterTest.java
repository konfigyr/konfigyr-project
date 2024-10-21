package com.konfigyr.security.provision;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisioningAuthenticationFilterTest {

	@Mock
	FilterChain chain;

	@Mock
	AuthenticationManager authenticationManager;

	@Mock
	AuthenticationSuccessHandler successHandler;

	@Mock
	AuthenticationFailureHandler failureHandler;

	ProvisioningAuthenticationFilter filter;
	MockHttpServletRequest request;
	MockHttpServletResponse response;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		filter = new ProvisioningAuthenticationFilter();
		filter.setFilterProcessesUrl("/account-provisioned");
		filter.setAuthenticationManager(authenticationManager);
		filter.setAuthenticationSuccessHandler(successHandler);
		filter.setAuthenticationFailureHandler(failureHandler);
		filter.afterPropertiesSet();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("should not authenticate when not using forward dispatch")
	void shouldNotAuthenticateWithoutForward() throws Exception {
		request.setPathInfo("/account-provisioned");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(successHandler);
		verifyNoInteractions(failureHandler);
		verifyNoInteractions(authenticationManager);
	}

	@Test
	@DisplayName("should not authenticate when not request does not match")
	void shouldNotAuthenticateWithoutMatchingPath() throws Exception {
		request.setDispatcherType(DispatcherType.FORWARD);
		request.setPathInfo("/account");

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(successHandler);
		verifyNoInteractions(failureHandler);
		verifyNoInteractions(authenticationManager);
	}

	@Test
	@DisplayName("should fail to authenticate when not account parameter is not present")
	void shouldFailWhenNoParameterIsPresent() throws Exception {
		request.setDispatcherType(DispatcherType.FORWARD);
		request.setPathInfo("/account-provisioned");

		filter.doFilter(request, response, chain);

		verifyNoInteractions(chain);
		verifyNoInteractions(successHandler);
		verifyNoInteractions(authenticationManager);

		verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), assertArg(ex -> assertThat(ex)
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class)
				.hasMessageContaining("Failed to extract account identifier from the request")
				.hasNoCause()
		));
	}

	@Test
	@DisplayName("should authenticate account when parameter is present")
	void shouldPerformAuthentication() throws Exception {
		request.setDispatcherType(DispatcherType.FORWARD);
		request.setPathInfo("/account-provisioned");
		request.setParameter("account", "user-account-hash");

		final Authentication result = mock(Authentication.class);
		doReturn(result).when(authenticationManager).authenticate(any());

		filter.doFilter(request, response, chain);

		verifyNoInteractions(chain);
		verifyNoInteractions(failureHandler);

		verify(authenticationManager).authenticate(assertArg(authentication -> assertThat(authentication)
				.isInstanceOf(PreAuthenticatedAuthenticationToken.class)
				.returns("user-account-hash", Authentication::getPrincipal)
				.returns("N/A", Authentication::getCredentials)
				.returns(AuthorityUtils.NO_AUTHORITIES, Authentication::getAuthorities)
				.matches(it -> Objects.nonNull(it.getDetails()))
		));

		verify(successHandler).onAuthenticationSuccess(request, response, result);
	}

}
