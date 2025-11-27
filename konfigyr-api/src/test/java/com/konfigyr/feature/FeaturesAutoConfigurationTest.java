package com.konfigyr.feature;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.jspecify.annotations.NonNull;

import static org.assertj.core.api.Assertions.*;

class FeaturesAutoConfigurationTest {

	final Configurations configurations = AutoConfigurations.of(
			FeaturesAutoConfiguration.class
	);

	ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner()
				.withConfiguration(configurations);
	}

	@Test
	@DisplayName("should register feature definitions beans")
	void shouldRegisterFeatures() {
		runner.withBean(FeatureDefinitionConfigurer.class, () -> configurerFor(TestFeatureDefinitions.LIMITED))
				.run(context -> {
					assertThat(context)
							.hasNotFailed()
							.hasSingleBean(FeatureDefinitions.class)
							.hasSingleBean(Features.class);

					assertThat(context.getBean(FeatureDefinitions.class))
							.hasSize(1)
							.containsExactlyInAnyOrder(TestFeatureDefinitions.LIMITED);
				});
	}

	@Test
	@DisplayName("should fail to register same feature twice")
	void shouldRegisterFeaturesTwice() {
		runner.withBean("firstConfigurer", FeatureDefinitionConfigurer.class, () -> configurerFor(TestFeatureDefinitions.LIMITED))
				.withBean("secondConfigurer", FeatureDefinitionConfigurer.class, () -> configurerFor(TestFeatureDefinitions.LIMITED))
				.withBean(DSLContext.class, () -> Mockito.mock(DSLContext.class))
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.rootCause()
						.hasMessageContaining("You have attempted to register a Feature Definition that is already registered")
				);
	}

	static FeatureDefinitionConfigurer configurerFor(@NonNull FeatureDefinition<?> definition) {
		return definitions -> definitions.add(definition);
	}

}
