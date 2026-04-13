package com.konfigyr.vault;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;

/**
 * Abstract event type that should be used for all {@link ChangeRequestEvent} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed class ChangeRequestEvent extends EntityEvent permits
		ChangeRequestEvent.Opened, ChangeRequestEvent.Merged, ChangeRequestEvent.Discarded {

	/**
	 * Creates a new {@link ChangeRequestEvent} for the given {@link EntityId} of the {@link ChangeRequest}
	 *
	 * @param id the entity identifier of change request that is the event subject, cannot be {@literal null}.
	 */
	protected ChangeRequestEvent(EntityId id) {
		super(id);
	}

	/**
	 * Vault event that is published when {@link ChangeRequest} is successfully opened by the {@link Vault}.
	 */
	public static final class Opened extends ChangeRequestEvent {

		/**
		 * Create a new {@link ChangeRequestEvent.Discarded} event with the {@link EntityId entity identifier}
		 * of the {@link ChangeRequest} that was just opened.
		 *
		 * @param id the entity identifier of the opened change request, cannot be {@literal null}.
		 */
		public Opened(EntityId id) {
			super(id);
		}

	}

	/**
	 * Vault event that is published when {@link ChangeRequest} is successfully merged to the target
	 * {@link Profile} by the {@link Vault}.
	 */
	public static final class Merged extends ChangeRequestEvent {

		private final ApplyResult result;

		/**
		 * Create a new {@link ChangeRequestEvent.Merged} event with the {@link EntityId entity identifier}
		 * of the {@link ChangeRequest} that was merged and the {@link ApplyResult} of the property changes.
		 *
		 * @param id the entity identifier of the merged change request, cannot be {@literal null}.
		 * @param result the result of the changes that were applied to the target profile, cannot be {@literal null}.
		 */
		public Merged(EntityId id, ApplyResult result) {
			super(id);
			this.result = result;
		}

		/**
		 * Returns the {@link ApplyResult} of the property changes that were applied to the {@link Vault}.
		 *
		 * @return the result of the changes that were applied, never {@literal null}.
		 */
		@NonNull
		public ApplyResult result() {
			return result;
		}

	}

	/**
	 * Vault event that is published when {@link ChangeRequest} is successfully discarded by the {@link Vault}.
	 */
	public static final class Discarded extends ChangeRequestEvent {

		/**
		 * Create a new {@link ChangeRequestEvent.Discarded} event with the {@link EntityId entity identifier}
		 * of the {@link ChangeRequest} that was discarded.
		 *
		 * @param id the entity identifier of the discarded change request, cannot be {@literal null}.
		 */
		public Discarded(EntityId id) {
			super(id);
		}

	}

}
