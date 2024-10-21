package com.konfigyr.security.access;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.account.Memberships;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.test.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MethodSecurityExpressionHandlerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	AuthorizationTestController service;

	Namespace namespace;

	@BeforeEach
	void setup() {
		namespace = namespaces.findBySlug("konfigyr").orElseThrow();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("service should allow access to method for members")
	void shouldAllowMembers() {
		authenticated(TestPrincipals.john());

		assertThatNoException().isThrownBy(() -> service.members(namespace));
	}

	@Test
	@DisplayName("service should allow access to method for administrators")
	void shouldAllowAdmins() {
		authenticated(TestPrincipals.john());

		assertThatNoException().isThrownBy(() -> service.admins(namespace));
	}

	@Test
	@DisplayName("service should not allow access to method for non-members")
	void shouldDenyNonMembers() {
		final Account account = Account.builder().id(3L)
				.status(AccountStatus.ACTIVE)
				.email("stilgar@freemen.com")
				.firstName("Stilgar")
				.memberships(Memberships.empty())
				.build();

		authenticated(TestPrincipals.from(account));

		assertThatThrownBy(() -> service.members(namespace))
				.isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	@DisplayName("service should not allow access to method for non-administrators")
	void shouldDenyNonAdmins() {
		authenticated(TestPrincipals.jane());

		assertThatThrownBy(() -> service.admins(namespace))
				.isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	@DisplayName("service should not allow access to methods with unsupported authentication type")
	void shouldDenyAccessToNonSupportedAuthenticationTypes() {
		authenticated(new TestingAuthenticationToken("test-user", "shhhh!"));

		assertThatThrownBy(() -> service.members(namespace))
				.isInstanceOf(AuthorizationDeniedException.class);

		assertThatThrownBy(() -> service.admins(namespace))
				.isInstanceOf(AuthorizationDeniedException.class);
	}

	@Test
	@DisplayName("service should not allow access to methods without authentication")
	void shouldDenyAccessWithoutAuthentication() {
		assertThatThrownBy(() -> service.members(namespace))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);

		assertThatThrownBy(() -> service.admins(namespace))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	private static void authenticated(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
