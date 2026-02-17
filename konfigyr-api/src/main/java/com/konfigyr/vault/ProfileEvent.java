package com.konfigyr.vault;


import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * Abstract event type that should be used for all {@link Profile} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed abstract class ProfileEvent extends EntityEvent implements Supplier<Profile>
		permits ProfileEvent.Created, ProfileEvent.Updated, ProfileEvent.Deleted {

	protected final Profile profile;

	protected ProfileEvent(Profile profile) {
		super(profile.id());
		this.profile = profile;
	}

	/**
	 * Returns the {@link Profile} that was affected by this event.
	 *
	 * @return the affected profile, never {@literal null}.
	 */
	@NonNull
	@Override
	public Profile get() {
		return profile;
	}

	/**
	 * Event that would be published when a new {@link Profile} is created.
	 */
	@DomainEvent(name = "profile-created", namespace = "vault")
	public static final class Created extends ProfileEvent {

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
		 * {@link Profile} that was just created by the {@link ProfileManager}.
		 *
		 * @param profile the created profile.
		 */
		public Created(Profile profile) {
			super(profile);
		}
	}

	/**
	 * Event that would be published when a {@link Profile} is updated.
	 */
	@DomainEvent(name = "profile-updated", namespace = "vault")
	public static final class Updated extends ProfileEvent {

		/**
		 * Create a new {@link Updated} event with the {@link EntityId entity identifier} of the
		 * {@link Profile} that was just updated.
		 *
		 * @param profile the updated profile
		 */
		public Updated(Profile profile) {
			super(profile);
		}
	}

	/**
	 * Event that would be published when an existing {@link Profile} is deleted.
	 */
	@DomainEvent(name = "profile-deleted", namespace = "vault")
	public static final class Deleted extends ProfileEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifier} of the
		 * {@link Profile} that was just deleted by the {@link ProfileManager}.
		 *
		 * @param profile the deleted profile.
		 */
		public Deleted(Profile profile) {
			super(profile);
		}
	}

}
