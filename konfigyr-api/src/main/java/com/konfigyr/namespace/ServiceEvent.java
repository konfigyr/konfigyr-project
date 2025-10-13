package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.lang.NonNull;

/**
 * Abstract event type that should be used for all {@link Service} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed abstract class ServiceEvent extends EntityEvent
		permits ServiceEvent.Created, ServiceEvent.Renamed, ServiceEvent.Deleted {

	protected ServiceEvent(EntityId id) {
		super(id);
	}

	/**
	 * Event that would be published when a new {@link Service} is created.
	 */
	@DomainEvent(name = "service-created", namespace = "namespaces")
	public static final class Created extends ServiceEvent {

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
		 * {@link Service} that was just created by the {@link Services service manager}.
		 *
		 * @param id entity identifier of the created service
		 */
		public Created(EntityId id) {
			super(id);
		}
	}

	/**
	 * Event that would be published when a {@link Service} URL slug is updated.
	 */
	@DomainEvent(name = "service-renamed", namespace = "namespaces")
	public static final class Renamed extends ServiceEvent {

		private final Slug from;

		private final Slug to;

		/**
		 * Create a new {@link Renamed} event with the {@link EntityId entity identifier} of the
		 * {@link Service} that was just updated and the URL slug values.
		 *
		 * @param id entity identifier of the update service
		 * @param from the previous service URL slug
		 * @param to the new service URL slug
		 */
		public Renamed(EntityId id, Slug from, Slug to) {
			super(id);
			this.from = from;
			this.to = to;
		}

		/**
		 * The previous URL slug that was used by the {@link Service}.
		 *
		 * @return previous URL slug, never {@literal null}
		 */
		@NonNull
		public Slug from() {
			return from;
		}

		/**
		 * The current URL slug that is set for the {@link Service}.
		 *
		 * @return current URL slug, never {@literal null}
		 */
		public Slug to() {
			return to;
		}
	}

	/**
	* Event that would be published when an existing {@link Service} is deleted.
	*/
	@DomainEvent(name = "service-deleted", namespace = "namespaces")
	public static final class Deleted extends ServiceEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifier} of the
		 * {@link Service} that was just deleted by the {@link Services service manager}.
		 *
		 * @param id entity identifier of the deleted service.
		 */
		public Deleted(EntityId id) {
			super(id);
		}
	}

}
