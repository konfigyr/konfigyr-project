package com.konfigyr.vault.environment;

import com.konfigyr.namespace.Service;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ConfigurationEnvironmentObservation {

	static final String OBSERVATION_NAME = "konfigyr.vault.environment.locator";

	static Observation create(ObservationRegistry registry, Service service) {
		return Observation.createNotStarted(Convention.INSTANCE, () -> new Context(service), registry);
	}

	static Observation.Event located(String profile) {
		return Observation.Event.of("konfigyr.vault.environment.located", profile);
	}

	static Observation.Event missing(String profile) {
		return Observation.Event.of("konfigyr.vault.environment.missing", profile);
	}

	static final class Context extends Observation.Context {
		private final Service service;

		Context(Service service) {
			this.service = service;
		}

		String id() {
			return service.id().serialize();
		}

		String name() {
			return service.slug();
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
			return "locating configuration environment for '" + context.name() + "' service";
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(Context context) {
			return KeyValues.of("konfigyr.namespace.service", context.id());
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof Context;
		}
	}

}
