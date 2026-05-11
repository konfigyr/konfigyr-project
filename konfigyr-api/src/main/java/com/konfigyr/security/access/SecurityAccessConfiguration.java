package com.konfigyr.security.access;

import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.FilterInvocation;

import java.util.Optional;

@NullMarked
@Configuration(proxyBeanMethods = false)
public class SecurityAccessConfiguration {

	@Bean
	AccessControlCache accessControlCache(ObjectProvider<CacheManager> cacheManager) {
		final Cache cache = Optional.ofNullable(cacheManager.getIfAvailable())
				.map(manager -> manager.getCache("access-control"))
				.orElseGet(() -> new NoOpCache("access-control"));

		return new AccessControlCache(cache);
	}

	@Bean
	AccessControlRepository accessControlRepository(DSLContext context) {
		return new AccessControlRepository(context);
	}

	@Bean
	AccessService konfigyrAccessService(AccessControlCache accessControlCache, AccessControlRepository accessControlRepository) {
		return new KonfigyrAccessService(accessControlCache, accessControlRepository);
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
