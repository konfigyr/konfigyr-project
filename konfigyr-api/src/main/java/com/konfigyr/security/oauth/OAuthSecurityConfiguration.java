package com.konfigyr.security.oauth;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;

@Configuration(proxyBeanMethods = false)
public class OAuthSecurityConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static Advisor requiresScope() {
		final AuthorizationManagerBeforeMethodInterceptor interceptor = new AuthorizationManagerBeforeMethodInterceptor(
				Pointcuts.union(
						new AnnotationMatchingPointcut(null, RequiresScope.class, true),
						new AnnotationMatchingPointcut(RequiresScope.class, true)
				),
				new RequiresScopeAuthorizationManager()
		);
		interceptor.setOrder(AuthorizationInterceptorsOrder.FIRST.getOrder());
		return interceptor;
	}
}
