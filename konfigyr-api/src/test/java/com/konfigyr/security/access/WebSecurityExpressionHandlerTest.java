package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WebSecurityExpressionHandlerTest {

	@Mock
	Namespace namespace;

	@Mock
	FilterInvocation ctx;

	@Mock
	AccessService accessService;

	@Mock
	AuthorizationResult result;

	KonfigyrWebSecurityExpressionHandler expressionHandler;

	@BeforeEach
	void setup() {
		expressionHandler = new KonfigyrWebSecurityExpressionHandler();
		expressionHandler.setAccessService(accessService);
		expressionHandler.afterPropertiesSet();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("service should allow access to method for members")
	void shouldAllowMembers() {
		doReturn(true).when(result).isGranted();
		doReturn(result).when(accessService).hasAccess(any(), eq("konfigyr"));

		assertThat(members(TestPrincipals.jane())).isTrue();

		verify(accessService).hasAccess(TestPrincipals.jane(), "konfigyr");
	}

	@Test
	@DisplayName("service should allow access to method for administrators")
	void shouldAllowAdmins() {
		doReturn(true).when(result).isGranted();
		doReturn(result).when(accessService).hasAccess(any(), eq("konfigyr"), eq(NamespaceRole.ADMIN));

		assertThat(admins(TestPrincipals.john())).isTrue();

		verify(accessService).hasAccess(TestPrincipals.john(), "konfigyr", NamespaceRole.ADMIN);
	}

	@Test
	@DisplayName("service should not allow access to method for non-members")
	void shouldDenyNonMembers() {
		doReturn(result).when(accessService).hasAccess(TestPrincipals.jane(), "konfigyr");

		assertThat(members(TestPrincipals.jane())).isFalse();

		verify(accessService).hasAccess(TestPrincipals.jane(), "konfigyr");
	}

	@Test
	@DisplayName("service should not allow access to method for non-administrators")
	void shouldDenyNonAdmins() {
		doReturn(result).when(accessService).hasAccess(TestPrincipals.john(), "konfigyr", NamespaceRole.ADMIN);

		assertThat(admins(TestPrincipals.john())).isFalse();

		verify(accessService).hasAccess(TestPrincipals.john(), "konfigyr", NamespaceRole.ADMIN);
	}

	private boolean members(Authentication authentication) {
		return evaluate(authentication, "isMember(#namespace)");
	}

	private boolean admins(Authentication authentication) {
		return evaluate(authentication, "isAdmin(#namespace)");
	}

	private boolean evaluate(Authentication authentication, String template) {
		doReturn("konfigyr").when(namespace).slug();

		final var context = expressionHandler.createEvaluationContext(authentication, ctx);
		context.setVariable("namespace", namespace);

		final var expression =  expressionHandler.getExpressionParser().parseExpression(template);
		final var result = expression.getValue(context, AuthorizationResult.class);

		return result != null && result.isGranted();
	}

}
