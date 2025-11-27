package com.konfigyr.security.access;

import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.FilterInvocation;

@NullMarked
@Configuration(proxyBeanMethods = false)
public class SecurityAccessConfiguration {

	@Bean
	AccessControlRepository accessControlRepository(DSLContext context) {
		return new AccessControlRepository(context);
	}

	@Bean
	AccessService konfigyrAccessService(AccessControlRepository repository, CacheManager manager) {
		return new KonfigyrAccessService(repository, manager.getCache("access-control"));
	}

	@Bean
	MethodSecurityExpressionHandler konfigyrMethodSecurityExpressionHandler(AccessService accessService) {
		final KonfigyrMethodSecurityExpressionHandler handler = new KonfigyrMethodSecurityExpressionHandler();
		handler.setAccessService(accessService);
		return handler;
	}

	@Bean
	SecurityExpressionHandler<FilterInvocation> konfigyrWebSecurityExpressionHandler(AccessService accessService) {
		final KonfigyrWebSecurityExpressionHandler handler = new KonfigyrWebSecurityExpressionHandler();
		handler.setAccessService(accessService);
		return handler;
	}

	@Bean
	WebSecurityCustomizer konfigyrSecurityCustomizer(
			SecurityExpressionHandler<FilterInvocation> konfigyrWebSecurityExpressionHandler
	) {
		return security -> security.expressionHandler(konfigyrWebSecurityExpressionHandler);
	}

}
