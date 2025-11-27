package com.konfigyr.security.oauth;

import com.konfigyr.security.OAuthScope;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

@ExtendWith(MockitoExtension.class)
class RequiresScopeAuthorizationManagerTest {

	final AuthorizationManager<MethodInvocation> manager = new RequiresScopeAuthorizationManager();
	final MethodInvocation invocation = createMethodInvocation("protect");

	@Test
	@DisplayName("should deny access when authentication is not present")
	void unauthenticated() {
		denied(null);
	}

	@Test
	@DisplayName("should grant access when method invocation is not resolved")
	void missingInvocation() {
		final var authentication = new TestingAuthenticationToken("john", "doe", List.of());

		assertThatNoException().isThrownBy(() -> manager.verify(() -> authentication, null));
	}

	@Test
	@DisplayName("should deny access when authentication has no authorities")
	void emptyAuthorities() {
		final var authentication = new TestingAuthenticationToken("john", "doe", List.of());

		denied(authentication);
	}

	@Test
	@DisplayName("should deny access when authentication has no scopes")
	void nonScopedAuthorities() {
		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(new SimpleGrantedAuthority("other-authority")));

		denied(authentication);
	}

	@Test
	@DisplayName("should deny access when authentication does not have required scope")
	void missingRequiredScope() {
		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(OAuthScope.INVITE_MEMBERS, OAuthScope.OPENID));

		denied(authentication);
	}

	@Test
	@DisplayName("should grant access when authentication contains required scope")
	void shouldGrantAccess() {
		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(OAuthScope.WRITE_NAMESPACES));

		assertThatNoException().isThrownBy(() -> manager.verify(() -> authentication, invocation));
	}

	@Test
	@DisplayName("should grant access when authentication contains mixed scope and other granted authorities")
	void shouldGrantAccessToMixedAuthorities() {
		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(OAuthScope.WRITE_NAMESPACES, new SimpleGrantedAuthority("other-authority")));

		assertThatNoException().isThrownBy(() -> manager.verify(() -> authentication, invocation));
	}

	@Test
	@DisplayName("should grant access when authentication contains inherited scope")
	void shouldGrantIndirectAccess() {
		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(OAuthScope.DELETE_NAMESPACES));

		assertThatNoException().isThrownBy(() -> manager.verify(() -> authentication, invocation));
	}

	@Test
	@DisplayName("should grant access when no required scopes can be found")
	void shouldGrantAccessForMissingScopes() {
		final var invocation = createMethodInvocation("missing");

		final var authentication = new TestingAuthenticationToken("john", "doe",
				List.of(OAuthScope.DELETE_NAMESPACES));

		assertThatNoException().isThrownBy(() -> manager.verify(() -> authentication, invocation));
	}

	void denied(@Nullable Authentication authentication) {
		assertThatExceptionOfType(AuthorizationDeniedException.class)
				.isThrownBy(() -> manager.verify(() -> authentication, invocation))
				.withMessageContaining("Access Denied")
				.withNoCause()
				.extracting(AuthorizationDeniedException::getAuthorizationResult)
				.returns(false, AuthorizationResult::isGranted)
				.isInstanceOf(AuthorityAuthorizationDecision.class)
				.asInstanceOf(type(AuthorityAuthorizationDecision.class))
				.returns(List.of(OAuthScope.WRITE_NAMESPACES), AuthorityAuthorizationDecision::getAuthorities);
	}

	private static MethodInvocation createMethodInvocation(String methodName) {
		final var target = new Protected();
		final var method = ReflectionUtils.findMethod(Protected.class, methodName);

		assertThat(method)
				.as("Method with name '%s' on a Protected subject type is not found", methodName)
				.isNotNull();

		return new SimpleMethodInvocation(target, method);
	}

	private static final class Protected {

		@RequiresScope(OAuthScope.WRITE_NAMESPACES)
		void protect() {
			// do something...
		}

		void missing() {
			// do nothing
		}

	}

}
