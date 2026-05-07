package com.konfigyr.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AuthenticatedPrincipalContextFilterTest {

	@Mock
	Authentication authentication;

	@Mock
	AuthenticatedPrincipal principal;

	AuthenticatedPrincipalContextFilter filter;
	MockHttpServletRequest request;
	MockHttpServletResponse response;

	@BeforeEach
	void setup() {
		filter = new AuthenticatedPrincipalContextFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
		MDC.clear();
	}

	@Test
	@DisplayName("should populate MDC with actor and actor type when principal is present")
	void shouldPopulateMdc() throws Exception {
		doReturn("user-subject-id").when(principal).get();
		doReturn(PrincipalType.USER_ACCOUNT).when(principal).getType();
		doReturn(principal).when(authentication).getPrincipal();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filter.doFilter(request, response, (_, _) -> {
			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
					.isEqualTo("user-subject-id");

			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_TYPE_KEY))
					.isEqualTo("USER_ACCOUNT");
		});

		assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
				.isNull();

		assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_TYPE_KEY))
				.isNull();
	}

	@Test
	@DisplayName("should not populate MDC when no authentication is present")
	void shouldNotPopulateMdcWhenUnauthenticated() throws Exception {
		filter.doFilter(request, response, (_, _) -> {
			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
					.isNull();

			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_TYPE_KEY))
					.isNull();
		});
	}

	@Test
	@DisplayName("should not populate MDC when principal is not an AuthenticatedPrincipal")
	void shouldNotPopulateMdcForNonAuthenticatedPrincipal() throws Exception {
		doReturn("plain-string-principal").when(authentication).getPrincipal();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filter.doFilter(request, response, (_, _) -> {
			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
					.isNull();

			assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_TYPE_KEY))
					.isNull();
		});
	}

	@Test
	@DisplayName("should clean up MDC even when filter chain throws an exception")
	void shouldCleanUpMdcOnException() {
		doReturn("user-subject-id").when(principal).get();
		doReturn(PrincipalType.USER_ACCOUNT).when(principal).getType();
		doReturn(principal).when(authentication).getPrincipal();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final var cause = new RuntimeException("filter chain failure");

		assertThatException().isThrownBy(() ->
				filter.doFilter(request, response, (_, _) -> {
					assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
							.isEqualTo("user-subject-id");
					throw cause;
				})
		).isEqualTo(cause);

		assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_KEY))
				.isNull();

		assertThat(MDC.get(AuthenticatedPrincipalContextFilter.ACTOR_TYPE_KEY))
				.isNull();
	}

}
