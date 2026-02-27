package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * Abstract event type that should be used for all {@link Service} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed abstract class ServiceEvent extends EntityEvent implements Supplier<Service>
		permits ServiceEvent.Created, ServiceEvent.Renamed, ServiceEvent.Deleted {

	protected final Service service;

	protected ServiceEvent(Service service) {
		super(service.id());
		this.service = service;
	}

	/**
	 * Returns the {@link Service} that was affected by this event.
	 *
	 * @return the affected service, never {@literal null}.
	 */
	@NonNull
	@Override
	public Service get() {
		return service;
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
		 * @param service the created service.
		 */
		public Created(Service service) {
			super(service);
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
		 * @param service the updated service containing the new URL slug.
		 * @param from the previous service URL slug
		 * @param to the new service URL slug
		 */
		public Renamed(Service service, Slug from, Slug to) {
			super(service);
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
		 * @param service the deleted service.
		 */
		public Deleted(Service service) {
			super(service);
		}
	}

}
