package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Abstract event type that should be used for all {@link Namespace} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class NamespaceEvent extends EntityEvent permits NamespaceEvent.Created {

	protected NamespaceEvent(EntityId id) {
		super(id);
	}

	/**
	 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
	 * {@link Namespace} that was just created by the {@link NamespaceManager}.
	 *
	 * @param id entity identifier of the created namespace
	 * @return namespace created event, never {@literal null}
	 */
	@NonNull
	public static Created created(EntityId id) {
		Assert.notNull(id, "Namespace entity identifier can not be null");
		return new Created(id);
	}

	/**
	 * Event that would be published when a new {@link Namespace} is created.
	 */
	public static final class Created extends NamespaceEvent {
		private Created(EntityId id) {
			super(id);
		}
	}

}
