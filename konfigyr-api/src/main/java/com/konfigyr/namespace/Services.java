package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Service namespace services}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Repository
public interface Services {

	/**
	 * Retrieve all {@link Service services} that are managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace for which services should be retrieved, can't be {@literal null}.
	 * @param query search query to be executed when searching for {@link Service services}, can't be {@literal null}.
	 * @return paged collections of services, never {@literal null}.
	 */
	@NonNull
	Page<Service> find(@NonNull Namespace namespace, @NonNull SearchQuery query);

	/**
	 * Retrieve a single {@link Service} by its entity identifier.
	 *
	 * @param id service entity identifier, can't be {@literal null}.
	 * @return found service or an empty {@link Optional}, never {@literal null}.
	 */
	@NonNull
	Optional<Service> get(@NonNull EntityId id);

	/**
	 * Retrieve a single {@link Service} by its slug that is managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}
	 * @param slug      service slug, can't be {@literal null}
	 * @return found service or an empty {@link Optional}, never {@literal null}
	 */
	@NonNull
	Optional<Service> get(@NonNull Namespace namespace, @NonNull String slug);

	/**
	 * Checks if a {@link Service} by its slug that is managed by the given {@link Namespace} exists.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}
	 * @param slug      service slug, can't be {@literal null}
	 * @return {@literal true} if there is a service with this slug, {@literal false} otherwise.
	 */
	boolean exists(@NonNull Namespace namespace, @NonNull String slug);

	/**
	 * Creates a new {@link Service} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link ServiceEvent.Created}
	 * when a {@link Service} was successfully created.
	 *
	 * @param definition definition used to create the service, can't be {@literal null}
	 * @return created service, never {@literal null}
	 * @throws ServiceExistsException when there is already a {@link Service} with the same slug
	 * @throws NamespaceNotFoundException when a {@link Namespace} does not exist
	 */
	@NonNull
	@DomainEventPublisher(publishes = "namespaces.service-created")
	Service create(@NonNull ServiceDefinition definition);

	/**
	 * Updates an existing {@link Service} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link NamespaceEvent.Renamed}
	 * when a {@link Namespace} slug was successfully updated.
	 *
	 * @param id service entity identifier, can't be {@literal null}
	 * @param definition definition used to update the service, can't be {@literal null}
	 * @return updated service, never {@literal null}
	 * @throws ServiceExistsException when there is already a {@link Service} with the same slug
	 * @throws ServiceNotFoundException when a {@link Service} with the given identifier does not exist
	 * @throws NamespaceNotFoundException when a {@link Namespace} does not exist
	 */
	@DomainEventPublisher(publishes = "namespaces.service-renamed")
	Service update(@NonNull EntityId id, @NonNull ServiceDefinition definition);

	/**
	 * Deletes a single {@link Service} by its entity identifier.
	 *
	 * @param id service entity identifier, can't be {@literal null}.
	 * @throws ServiceNotFoundException when a {@link Service} with the given entity identifier does not exist.
	 */
	@DomainEventPublisher(publishes = "namespace.service-deleted")
	void delete(@NonNull EntityId id);

	/**
	 * Deletes a single {@link Service} by its slug that is managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}.
	 * @param slug      service slug, can't be {@literal null}.
	 * @throws ServiceNotFoundException when a {@link Service} with the given slug does not exist
	 * within the given {@link Namespace}.
	 */
	@DomainEventPublisher(publishes = "namespace.service-deleted")
	void delete(@NonNull Namespace namespace, @NonNull String slug);

}
