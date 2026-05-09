package com.konfigyr.queue;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NullMarked;

@NullMarked
class QueueObservation {

	static final String CONSUME_OBSERVATION_NAME = "konfigyr.queue.consume";
	static final String PROCESS_OBSERVATION_NAME = "konfigyr.queue.process";

	static Observation consume(ObservationRegistry registry) {
		return Observation.createNotStarted(CONSUME_OBSERVATION_NAME, registry);
	}

	static Observation process(ObservationRegistry registry, QueuedTask task) {
		return Observation.createNotStarted(ProcessorConvention.INSTANCE, () -> new ProcessorContext(task), registry);
	}

	static final class ProcessorContext extends Observation.Context {
		private final QueuedTask task;

		ProcessorContext(QueuedTask task) {
			this.task = task;
		}

		String entity() {
			return task.entityId().serialize();
		}

		String queueName() {
			return task.queueName();
		}
	}
	
	static final class ProcessorConvention implements ObservationConvention<ProcessorContext> {

		static final ObservationConvention<ProcessorContext> INSTANCE = new ProcessorConvention();

		@Override
		public String getName() {
			return PROCESS_OBSERVATION_NAME;
		}

		@Override
		public String getContextualName(ProcessorContext context) {
			return "processing queued task in '" + context.queueName() + "' queue with '" + context.entity() + "' resource identifier";
		}

		@Override
		public KeyValues getLowCardinalityKeyValues(ProcessorContext context) {
			return KeyValues.of("konfigyr.queue.name", context.queueName());
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(ProcessorContext context) {
			return KeyValues.of("konfigyr.queue.entity", context.entity());
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof ProcessorContext;
		}
	}
	
}
