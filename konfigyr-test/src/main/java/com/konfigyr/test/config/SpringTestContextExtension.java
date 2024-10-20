package com.konfigyr.test.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JUnit Extension that would initialize the {@link SpringTestContext} fields in a test class.
 *
 * @author Vladimir Spasic
 **/
public class SpringTestContextExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) {
		TestSecurityContextHolder.clearContext();
		getContexts(context.getRequiredTestInstance()).forEach(SpringTestContext::close);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		final Object testInstance = context.getRequiredTestInstance();
		getContexts(testInstance).forEach((springTestContext) -> springTestContext.with(testInstance));
	}

	private static List<SpringTestContext> getContexts(Object test) {
		final List<SpringTestContext> contexts = new ArrayList<>();

		ReflectionUtils.doWithLocalFields(test.getClass(), field -> {
			if (ClassUtils.isAssignable(SpringTestContext.class, field.getType())) {
				ReflectionUtils.makeAccessible(field);
				final SpringTestContext context = (SpringTestContext) ReflectionUtils.getField(field, test);
				contexts.add(context);
			}
		});

		return contexts;
	}

}
