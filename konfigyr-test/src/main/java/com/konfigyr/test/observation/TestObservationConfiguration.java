package com.konfigyr.test.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.event.BeforeTestExecutionEvent;

/**
 * Internal configuration class used to provide a {@link TestObservationRegistry} to the
 * Spring {@link org.springframework.context.ApplicationContext}.
 * <p>
 * It also registers the {@link ApplicationListener} bean that would liste for the
 * {@link BeforeTestExecutionEvent} to reset the registry state after each test method.
 * <p>
 * This prevents {@code Cross-Test Contamination} where observations recorded in one test
 * could incorrectly influence the assertions of a subsequent test within the same Spring
 * {@link org.springframework.context.ApplicationContext}.
 * <p>
 * This is marked as a {@link TestConfiguration} to prevent it from being picked up by component
 * scanning in the main application.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TestObservationRegistry
 */
@TestConfiguration
class TestObservationConfiguration {

	@Bean
	TestObservationRegistry testObservationRegistry() {
		return TestObservationRegistry.create();
	}

	@Bean
	ApplicationListener<BeforeTestExecutionEvent> testObservationRegistryListener(TestObservationRegistry observationRegistry) {
		return ignore -> observationRegistry.clear();
	}

}
