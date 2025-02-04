package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.test.TestPrincipals;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MethodSecurityExpressionHandlerTest {

	@Mock
	Namespace namespace;

	@Mock
	MethodInvocation ctx;

	@Mock
	AccessService accessService;

	KonfigyrMethodSecurityExpressionHandler expressionHandler;

	@BeforeEach
	void setup() {
		expressionHandler = new KonfigyrMethodSecurityExpressionHandler();
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
		doReturn(true).when(accessService).hasAccess(any(), eq("konfigyr"));

		assertThat(members(TestPrincipals.jane())).isTrue();

		verify(accessService).hasAccess(TestPrincipals.jane(), "konfigyr");
	}

	@Test
	@DisplayName("service should allow access to method for administrators")
	void shouldAllowAdmins() {
		doReturn(true).when(accessService).hasAccess(any(), eq("konfigyr"), eq(NamespaceRole.ADMIN));

		assertThat(admins(TestPrincipals.john())).isTrue();

		verify(accessService).hasAccess(TestPrincipals.john(), "konfigyr", NamespaceRole.ADMIN);
	}

	@Test
	@DisplayName("service should not allow access to method for non-members")
	void shouldDenyNonMembers() {
		assertThat(members(TestPrincipals.jane())).isFalse();

		verify(accessService).hasAccess(TestPrincipals.jane(), "konfigyr");
	}

	@Test
	@DisplayName("service should not allow access to method for non-administrators")
	void shouldDenyNonAdmins() {
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

		doReturn(new TestInvocation()).when(ctx).getThis();
		doReturn(TestInvocation.class.getMethods()[0]).when(ctx).getMethod();

		final var context = expressionHandler.createEvaluationContext(authentication, ctx);
		context.setVariable("namespace", namespace);

		final var expression =  expressionHandler.getExpressionParser().parseExpression(template);

		return Boolean.TRUE.equals(expression.getValue(context));
	}

	static class TestInvocation {
		void invoke() { /* noop */ }
	}

}
