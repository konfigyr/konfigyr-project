package com.konfigyr.vault;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * Abstract event type that should be used for all {@link Vault} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed abstract class VaultEvent extends EntityEvent implements Supplier<Profile>
		permits VaultEvent.ChangesApplied {

	protected final Profile profile;

	/**
	 * Creates a new {@link VaultEvent} for the given {@link Profile} that owns the {@link Vault}.
	 *
	 * @param profile the profile that owns the vault, cannot be {@literal null}.
	 */
	protected VaultEvent(Profile profile) {
		super(profile.id());
		this.profile = profile;
	}

	/**
	 * Returns the {@link Profile} that is the owner of the {@link Vault} affected by this event.
	 *
	 * @return the owning profile, never {@literal null}.
	 */
	@Override
	public Profile get() {
		return profile;
	}

	/**
	 * Vault event that is published when {@link PropertyChanges} are successfully applied to the {@link Vault}.
	 */
	@DomainEvent(name = "changes-applied", namespace = "vault")
	public static final class ChangesApplied extends VaultEvent {

		private final ApplyResult result;

		/**
		 * Create a new {@link ChangesApplied} event with the {@link EntityId entity identifier} of the
		 * {@link Profile} that owns the {@link Vault} and the {@link ApplyResult} of the property changes.
		 *
		 * @param profile the profile that owns the vault, cannot be {@literal null}.
		 * @param result the result of the changes that were applied
		 */
		public ChangesApplied(Profile profile, ApplyResult result) {
			super(profile);
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
