package com.konfigyr.config;

import com.konfigyr.test.config.SpringTestContext;
import com.konfigyr.test.config.SpringTestContextExtension;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringTestContextExtension.class)
class SpringTestContextTest {

	SpringTestContext context = SpringTestContext.create();

	@Autowired
	TestBean testBean;

	@Test
	void shouldCreateContext() {
		context.register(TestConfig.class).autowire();

		assertThat(testBean)
				.isNotNull()
				.isEqualTo(context.get(TestBean.class));
	}

	@Test
	void shouldApplyPostprocessors() {
		final var instance = new TestBean();

		context.postProcessor(ctx -> ctx.getBeanFactory()
				.registerResolvableDependency(TestBean.class, instance)
		).autowire();

		assertThat(testBean)
				.isNotNull()
				.isEqualTo(instance);
	}

	static class TestConfig {
		@Bean
		TestBean testBean() {
			return new TestBean();
		}
	}

	@Value
	static class TestBean {

	}
}
