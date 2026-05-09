package com.konfigyr.queue;

import io.micrometer.observation.ObservationRegistry;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class WorkerQueueAutoConfiguration {

	@Bean
	@ConditionalOnBean(QueueProcessorRegistration.class)
	QueueRegistrar workerQueueRegistrar(ObjectProvider<QueueProcessorRegistration> registrations) {
		return QueueRegistrar.of(registrations);
	}

	@Bean
	@ConditionalOnBean(QueueRegistrar.class)
	WorkerQueue workerQueue(DSLContext context, QueueRegistrar registrar) {
		return new WorkerQueue(context, registrar);
	}

	@Bean
	@ConditionalOnBean(QueueRegistrar.class)
	WorkerQueueScheduler workerQueueScheduler(WorkerQueue queue, QueueRegistrar registrar, ObservationRegistry registry) {
		return new WorkerQueueScheduler(queue, registrar, registry);
	}

}
