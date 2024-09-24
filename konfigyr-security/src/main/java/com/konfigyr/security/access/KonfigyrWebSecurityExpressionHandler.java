package com.konfigyr.security.access;

import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

/**
 * A {@link SecurityExpressionHandler} that uses a {@link FilterInvocation} to create
 * a {@link KonfigyrWebSecurityExpressionRoot}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class KonfigyrWebSecurityExpressionHandler extends AbstractSecurityExpressionHandler<FilterInvocation>
		implements SecurityExpressionHandler<FilterInvocation> {

	private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

	@Override
	protected SecurityExpressionOperations createSecurityExpressionRoot(
			Authentication authentication, FilterInvocation invocation) {
		final WebSecurityExpressionRoot root = new KonfigyrWebSecurityExpressionRoot(authentication, invocation);
		root.setRoleHierarchy(getRoleHierarchy());
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setTrustResolver(trustResolver);
		return root;
	}

}
