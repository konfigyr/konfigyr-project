package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@NullMarked
final class GatekeeperObservation {

	static final String OBSERVATION_NAME = "konfigyr.vault.gatekeeper";

	private GatekeeperObservation() {
		// prevent instantiation
	}

	static ChangeRequestMergeStatus decorate(ObservationRegistry registry, EntityId id, Supplier<ChangeRequestMergeStatus> evaluator) {
		final Observation observation = Observation.createNotStarted(Convention.INSTANCE, () -> new Context(id), registry);

		return observation.observe(() -> {
			final ChangeRequestMergeStatus status = evaluator.get();
			final Context context = (Context) observation.getContext();
			context.status(status);
			return status;
		});
	}

	static final class Context extends Observation.Context {
		private final EntityId id;
		private @Nullable ChangeRequestMergeStatus status;

		Context(EntityId id) {
			this.id = id;
		}

		void status(@Nullable ChangeRequestMergeStatus status) {
			this.status = status;
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
			return "evaluation of merge status for change request: " + context.id.serialize();
		}

		@Override
		public KeyValues getHighCardinalityKeyValues(Context context) {
			return KeyValues.of("konfigyr.vault.change-request", context.id.serialize());
		}

		@Override
		public KeyValues getLowCardinalityKeyValues(Context context) {
			return KeyValues.of("konfigyr.vault.change-request.merge-status",
					context.status == null ? "n/a" : context.status.name());
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof Context;
		}
	}
}
