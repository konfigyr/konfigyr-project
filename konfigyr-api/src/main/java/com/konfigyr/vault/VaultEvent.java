package com.konfigyr.vault;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;

/**
 * Abstract event type that should be used for all {@link Vault} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed abstract class VaultEvent extends EntityEvent permits VaultEvent.ChangesApplied {

	/**
	 * Creates a new {@link VaultEvent} for the given {@link EntityId} of the {@link Profile}
	 * that owns the {@link Vault}.
	 *
	 * @param id the entity identifier of profile that owns the vault, cannot be {@literal null}.
	 */
	protected VaultEvent(EntityId id) {
		super(id);
	}

	/**
	 * Vault event that is published when {@link PropertyChanges} are successfully applied to the {@link Vault}.
	 */
	public static final class ChangesApplied extends VaultEvent {

		private final ApplyResult result;

		/**
		 * Create a new {@link ChangesApplied} event with the {@link EntityId entity identifier} of the
		 * {@link Profile} that owns the {@link Vault} and the {@link ApplyResult} of the property changes.
		 *
		 * @param id the entity identifier of profile that owns the vault, cannot be {@literal null}.
		 * @param result the result of the changes that were applied
		 */
		public ChangesApplied(EntityId id, ApplyResult result) {
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

}
