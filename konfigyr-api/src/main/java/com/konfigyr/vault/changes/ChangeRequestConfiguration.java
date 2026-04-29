package com.konfigyr.vault.changes;

import com.konfigyr.queue.QueueProcessorRegistration;
import com.konfigyr.vault.gatekeeper.ChangeRequestGatekeeper;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class ChangeRequestConfiguration {

	private final DSLContext context;

	@Bean
	ChangeRequestManager changeRequestManager(ApplicationEventPublisher applicationEventPublisher) {
		return new ChangeRequestManager(context, applicationEventPublisher);
	}

	@Bean
	ChangeRequestEvaluator changeRequestEvaluator(ChangeRequestGatekeeper gatekeeper) {
		return new ChangeRequestEvaluator(context, gatekeeper);
	}

	@Bean
	ChangeRequestEvaluationQueueListener changeRequestEvaluationQueueListener() {
		return new ChangeRequestEvaluationQueueListener(context);
	}

	@Bean
	QueueProcessorRegistration changeRequestEvaluationQueueProcessorRegistration(ChangeRequestEvaluator evaluator) {
		return QueueProcessorRegistration.of(ChangeRequestEvaluationQueueListener.QUEUE_NAME, evaluator::evaluate)
				.backoff(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD)
				.timeout(ChangeRequestEvaluationQueueListener.TIMEOUT_PERIOD);
	}

}
