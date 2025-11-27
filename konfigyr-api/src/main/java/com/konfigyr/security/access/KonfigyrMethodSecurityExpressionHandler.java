package com.konfigyr.security.access;

import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Custom extension of the {@link DefaultMethodSecurityExpressionHandler} that should create our
 * custom {@link KonfigyrMethodSecurityExpressionRoot} that can be used to evaluate Konfigyr specific
 * Spring Security expressions.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see KonfigyrMethodSecurityExpressionRoot
 **/
@Setter
public class KonfigyrMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler implements InitializingBean {

	private AccessService accessService;

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(accessService, "Access service must not be null");
	}

	@Override
	@NullMarked
	public EvaluationContext createEvaluationContext(
			Supplier<? extends @Nullable Authentication> authentication,
			MethodInvocation invocation
	) {
		final KonfigyrMethodSecurityExpressionRoot root = new KonfigyrMethodSecurityExpressionRoot(
				accessService, authentication, invocation
		);
		setupSecurityExpressionRoot(root, invocation);

		final StandardEvaluationContext context = new MethodBasedEvaluationContext(root, getSpecificMethod(invocation),
				invocation.getArguments(), getParameterNameDiscoverer());
		context.setBeanResolver(getBeanResolver());

		return context;
	}

	@Override
	@NullMarked
	protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
			@Nullable Authentication authentication, MethodInvocation invocation) {
		final KonfigyrMethodSecurityExpressionRoot root = new KonfigyrMethodSecurityExpressionRoot(
				accessService, authentication, invocation
		);
		setupSecurityExpressionRoot(root, invocation);
		return root;
	}

	private void setupSecurityExpressionRoot(KonfigyrMethodSecurityExpressionRoot root, MethodInvocation mi) {
		root.setTarget(mi.getThis());
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
	}

	private static Method getSpecificMethod(MethodInvocation mi) {
		Assert.notNull(mi.getThis(), "Method invocation target class can not be null");
		return AopUtils.getMostSpecificMethod(mi.getMethod(), AopProxyUtils.ultimateTargetClass(mi.getThis()));
	}

}
