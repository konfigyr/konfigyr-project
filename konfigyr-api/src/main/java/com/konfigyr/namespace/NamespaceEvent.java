package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEvent;

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
	 * Event that would be published when a new {@link Namespace} is created.
	 */
	@DomainEvent(name = "created", namespace = "namespaces")
	public static final class Created extends NamespaceEvent {

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just created by the {@link NamespaceManager}.
		 *
		 * @param id entity identifier of the created namespace
		 */
		public Created(EntityId id) {
			super(id);
		}
	}

}
