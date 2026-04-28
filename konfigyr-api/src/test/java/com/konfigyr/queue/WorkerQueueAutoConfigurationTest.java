package com.konfigyr.queue;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerQueueAutoConfigurationTest {

	@Mock
	DSLContext context;

	ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		runner = new ApplicationContextRunner()
				.withBean(DSLContext.class, () -> context)
				.withConfiguration(AutoConfigurations.of(WorkerQueueAutoConfiguration.class));
	}

	@Test
	@DisplayName("should register queue registrar, worker queue and scheduler")
	void withRegistrations() {
		runner.with(registrationFor("test-queue")).run(context -> assertThat(context)
				.hasSingleBean(QueueRegistrar.class)
				.hasSingleBean(WorkerQueue.class)
				.hasSingleBean(WorkerQueueScheduler.class)
		);
	}

	@Test
	@DisplayName("should fail to start context when duplicate queue registrations are present")
	void withDuplicateRegistrations() {
		runner.with(registrationFor("test-queue"))
				.with(registrationFor("duplicate-queue"))
				.with(registrationFor("duplicate-queue"))
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasRootCauseMessage(
								"Queue processor registration for queue 'duplicate-queue' is already registered. " +
										"Please make sure that your registrations for queues are unique."
						)
				);
	}

	@Test
	@DisplayName("should not register any beans when no processor queue processor registrations are present")
	void withoutRegistrations() {
		runner.run(context -> assertThat(context)
				.doesNotHaveBean(QueueRegistrar.class)
				.doesNotHaveBean(WorkerQueue.class)
				.doesNotHaveBean(WorkerQueueScheduler.class)
		);
	}

	static Function<ApplicationContextRunner, ApplicationContextRunner> registrationFor(String queueName) {
		return runner -> runner.withBean(
				String.format("processor-registration-%s-%s", queueName, UUID.randomUUID()),
				QueueProcessorRegistration.class,
				() -> QueueProcessorRegistration.of(queueName, mock(QueueProcessor.class))
		);
	}

}
