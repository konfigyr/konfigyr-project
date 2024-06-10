package com.konfigyr.config;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Test context that can be used in JUnit tests to create and initialize a new
 * {@link org.springframework.web.context.WebApplicationContext} for each test execution.
 *
 * @author Vladimir Spasic
 * @see SpringTestContextExtension
 **/
public class SpringTestContext implements Supplier<ConfigurableWebApplicationContext>, Closeable {

	private Object test;
	private ConfigurableWebApplicationContext context;
	private AutowiredAnnotationBeanPostProcessor processor;
	private final List<ApplicationContextPostProcessor> postProcessors = new ArrayList<>();

	protected SpringTestContext() {
		// hide constructor
	}

	public static SpringTestContext create() {
		return new SpringTestContext();
	}

	void with(Object test) {
		Assert.notNull(test, "Test instance can not be null");
		this.test = test;
	}

	@Override
	public void close() {
		try {
			context.close();
		} catch (Exception ex) {
			// noop
		}
	}

	/**
	 * Register one or more component classes to be processed by this test context.
	 *
	 * @param classes component classes to be registered
	 * @return spring test context, never {@code null}
	 */
	@NonNull
	public SpringTestContext register(Class<?>... classes) {
		final AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		applicationContext.register(classes);
		context = applicationContext;
		return this;
	}

	/**
	 * Register your {@link AutowiredAnnotationBeanPostProcessor} that would be used to customize the
	 * {@link ConfigurableWebApplicationContext}.
	 *
	 * @param postProcessor webapp context post processor
	 * @return spring test context, never {@code null}
	 * @throws IllegalArgumentException when post process is {@code null}
	 */
	@NonNull
	public SpringTestContext postProcessor(ApplicationContextPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "Post processor can not be null");
		postProcessors.add(postProcessor);
		return this;
	}

	@NonNull
	public ConfigurableWebApplicationContext get() {
		if (!context.isRunning()) {
			setup();
		}
		return context;
	}

	/**
	 * Return the Spring Bean instance that uniquely matches the given object type.
	 *
	 * @param beanType Spring bean type, can't be {@code null}
	 * @param <T> Generic bean type
	 * @return matching Spring Bean, never {@code null}
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 */
	public <T> T get(@NonNull Class<T> beanType) {
		return get().getBean(beanType);
	}

	/**
	 * Call this method when you wish to initialize the application context and to autowire your
	 * JUnit test class.
	 */
	public void autowire() {
		setup();

		processor.processInjection(test);
	}

	private void setup() {
		if (context == null) {
			context = new AnnotationConfigWebApplicationContext();
		}

		context.setServletContext(new MockServletContext());
		context.setServletConfig(new MockServletConfig());

		context.refresh();

		if (processor == null) {
			processor = new AutowiredAnnotationBeanPostProcessor();
		}

		processor.setBeanFactory(context.getBeanFactory());

		for (ApplicationContextPostProcessor postProcessor : postProcessors) {
			postProcessor.process(context);
		}

		context.refresh();
	}

	@FunctionalInterface
	public interface ApplicationContextPostProcessor {
		void process(@NonNull ConfigurableWebApplicationContext context);
	}
}
