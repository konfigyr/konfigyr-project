package com.konfigyr.security.access;

import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;
import org.springframework.util.Assert;

/**
 * A {@link SecurityExpressionHandler} that uses a {@link FilterInvocation} to create
 * a {@link KonfigyrWebSecurityExpressionRoot}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Setter
public class KonfigyrWebSecurityExpressionHandler extends AbstractSecurityExpressionHandler<FilterInvocation>
		implements SecurityExpressionHandler<FilterInvocation>, InitializingBean {

	private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

	private AccessService accessService;

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(accessService, "Access service must not be null");
	}

	@Override
	protected SecurityExpressionOperations createSecurityExpressionRoot(
			Authentication authentication, FilterInvocation invocation) {
		final WebSecurityExpressionRoot root = new KonfigyrWebSecurityExpressionRoot(accessService, authentication, invocation);
		root.setRoleHierarchy(getRoleHierarchy());
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setTrustResolver(trustResolver);
		return root;
	}

}
