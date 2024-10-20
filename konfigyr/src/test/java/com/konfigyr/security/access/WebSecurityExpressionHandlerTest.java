package com.konfigyr.security.access;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountStatus;
import com.konfigyr.account.Memberships;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;

import static org.mockito.Mockito.doReturn;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class WebSecurityExpressionHandlerTest {

	@Mock
	Namespace namespace;

	@Mock
	FilterInvocation ctx;

	SecurityExpressionHandler<FilterInvocation> expressionHandler;

	@BeforeEach
	void setup() {
		expressionHandler = new KonfigyrWebSecurityExpressionHandler();
	}

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("service should allow access to method for members")
	void shouldAllowMembers() {
		assertThat(members(TestPrincipals.jane())).isTrue();
	}

	@Test
	@DisplayName("service should allow access to method for administrators")
	void shouldAllowAdmins() {
		assertThat(admins(TestPrincipals.john())).isTrue();
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

		assertThat(members(TestPrincipals.from(account))).isFalse();
		assertThat(admins(TestPrincipals.from(account))).isFalse();
	}

	@Test
	@DisplayName("service should not allow access to method for non-administrators")
	void shouldDenyNonAdmins() {
		assertThat(admins(TestPrincipals.jane())).isFalse();
	}

	@Test
	@DisplayName("service should not allow access to methods with unsupported authentication type")
	void shouldDenyAccessToNonSupportedAuthenticationTypes() {
		final var authentication = new TestingAuthenticationToken("test-user", "shhhh!");

		assertThat(members(authentication)).isFalse();
		assertThat(admins(authentication)).isFalse();
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

		return Boolean.TRUE.equals(expression.getValue(context));
	}

}
