package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.lang.NonNull;

/**
 * Abstract event type that should be used for all {@link Namespace} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class NamespaceEvent extends EntityEvent
		permits NamespaceEvent.Created, NamespaceEvent.Renamed, NamespaceEvent.Deleted {

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

	/**
	 * Event that would be published when a {@link Namespace} URL slug is updated.
	 */
	@DomainEvent(name = "renamed", namespace = "namespaces")
	public static final class Renamed extends NamespaceEvent {

		private final Slug from;

		private final Slug to;

		/**
		 * Create a new {@link Renamed} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just updated and the URL slug values.
		 *
		 * @param id entity identifier of the created namespace
		 * @param from the previous namespace URL slug
		 * @param to the new namespace URL slug
		 */
		public Renamed(EntityId id, Slug from, Slug to) {
			super(id);
			this.from = from;
			this.to = to;
		}

		/**
		 * The previous URL slug that was used by the {@link Namespace}.
		 *
		 * @return previous URL slug, never {@literal null}
		 */
		@NonNull
		public Slug from() {
			return from;
		}

		/**
		 * The current URL slug that is set for the {@link Namespace}.
		 *
		 * @return current URL slug, never {@literal null}
		 */
		public Slug to() {
			return to;
		}
	}

	/**
	 * Event that would be published when a new {@link Namespace} is deleted.
	 */
	@DomainEvent(name = "deleted", namespace = "namespaces")
	public static final class Deleted extends NamespaceEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just deleted by the {@link NamespaceManager}.
		 *
		 * @param id entity identifier of the deleted namespace
		 */
		public Deleted(EntityId id) {
			super(id);
		}
	}

}
