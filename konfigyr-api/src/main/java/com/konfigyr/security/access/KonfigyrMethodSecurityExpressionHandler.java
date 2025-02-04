package com.konfigyr.security.access;

import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
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
	public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
		final KonfigyrMethodSecurityExpressionRoot root = new KonfigyrMethodSecurityExpressionRoot(accessService, authentication);
		setupSecurityExpressionRoot(root, mi);

		final StandardEvaluationContext context = new MethodBasedEvaluationContext(root, getSpecificMethod(mi),
				mi.getArguments(), getParameterNameDiscoverer());
		context.setBeanResolver(getBeanResolver());

		return context;
	}

	@Override
	protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
			Authentication authentication, MethodInvocation invocation) {
		final KonfigyrMethodSecurityExpressionRoot root = new KonfigyrMethodSecurityExpressionRoot(accessService, authentication);
		setupSecurityExpressionRoot(root, invocation);
		return root;
	}

	private void setupSecurityExpressionRoot(KonfigyrMethodSecurityExpressionRoot root, MethodInvocation mi) {
		root.setTarget(mi.getThis());
		root.setPermissionEvaluator(getPermissionEvaluator());
		root.setTrustResolver(getTrustResolver());
		root.setRoleHierarchy(getRoleHierarchy());
		root.setDefaultRolePrefix(getDefaultRolePrefix());
	}

	private static Method getSpecificMethod(MethodInvocation mi) {
		Assert.notNull(mi.getThis(), "Method invocation target class can not be null");
		return AopUtils.getMostSpecificMethod(mi.getMethod(), AopProxyUtils.ultimateTargetClass(mi.getThis()));
	}

}
