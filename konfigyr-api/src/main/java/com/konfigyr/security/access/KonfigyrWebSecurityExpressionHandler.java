package com.konfigyr.security.access;

import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.expression.AbstractSecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.util.Assert;

/**
 * A {@link SecurityExpressionHandler} that uses a {@link FilterInvocation} to create
 * a {@link KonfigyrWebSecurityExpressionRoot}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Setter
@NullMarked
public class KonfigyrWebSecurityExpressionHandler extends AbstractSecurityExpressionHandler<FilterInvocation>
		implements SecurityExpressionHandler<FilterInvocation>, InitializingBean {

	private AccessService accessService;

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(accessService, "Access service must not be null");
	}

	@Override
	protected SecurityExpressionOperations createSecurityExpressionRoot(
			@Nullable Authentication authentication, FilterInvocation invocation) {
		final KonfigyrWebSecurityExpressionRoot root = new KonfigyrWebSecurityExpressionRoot(accessService, authentication, invocation);
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
		return root;
	}

}
