package com.konfigyr.audit;

import com.konfigyr.entity.EntityEvent;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
final class AuditObservation {

	private static final Map<Class<?>, String> EVENT_TYPE_CACHE = new ConcurrentHashMap<>(16);

	static final String OBSERVATION_NAME = "konfigyr.audit.event.listener";

	private AuditObservation() {
		// prevent instantiation
	}

	static Observation create(ObservationRegistry registry, EntityEvent event) {
		return Observation.createNotStarted(Convention.INSTANCE, () -> new Context(event), registry);
	}

	static final class Context extends Observation.Context {
		private final String id;
		private final String eventType;

		Context(EntityEvent event) {
			this.id = event.id().serialize();
			this.eventType = EVENT_TYPE_CACHE.computeIfAbsent(event.getClass(), Context::resolveEventType);
		}

		static String resolveEventType(Class<?> eventType) {
			final DomainEvent event = AnnotationUtils.findAnnotation(eventType, DomainEvent.class);
			Assert.notNull(event, "The event type " + eventType + " is not annotated with @DomainEvent");
			Assert.hasText(event.namespace(), "You must provide a namespace for the event type " + eventType);
			Assert.hasText(event.name(), "You must provide a name for the event type " + eventType);

			return String.format("%s.%s", event.namespace(), event.name());
		}
	}

	static final class Convention implements ObservationConvention<Context> {

		static final ObservationConvention<Context> INSTANCE = new Convention();

		@Override
		public String getName() {
			return OBSERVATION_NAME;
		}

		@Override
		public String getContextualName(Context context) {
			return "audit listener event with '" + context.eventType + "' type and '" + context.id + "' resource identifier";
		}

		@Override
		public KeyValues getLowCardinalityKeyValues(Context context) {
			return KeyValues.of("konfigyr.audit.event.type", context.eventType);
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(Context context) {
			return KeyValues.of("konfigyr.audit.event.id", context.id);
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof Context;
		}
	}

}
