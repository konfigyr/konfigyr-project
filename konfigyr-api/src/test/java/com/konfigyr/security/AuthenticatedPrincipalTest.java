package com.konfigyr.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedPrincipalTest {

	@Mock
	Authentication authentication;

	@Mock
	AuthenticatedPrincipal principal;

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("should resolve the Authenticated Principal from Spring security context")
	void resolvePrincipal() {
		doReturn(principal).when(authentication).getPrincipal();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertThat(AuthenticatedPrincipal.resolve())
				.isSameAs(principal);
	}

	@Test
	@DisplayName("should fail to resolve the Authenticated Principal from Spring security context")
	void resolveUnknownPrincipal() {
		doReturn("authenticated-principal").when(authentication).getPrincipal();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
				.isThrownBy(AuthenticatedPrincipal::resolve)
				.withMessage("Could not find authenticated principal in the current security context.")
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to resolve the Authenticated Principal from unauthenticated Spring security context")
	void resolveMissingPrincipal() {
		assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
				.isThrownBy(AuthenticatedPrincipal::resolve)
				.withMessage("Could not find authenticated principal in the current security context.")
				.withNoCause();
	}

}
