package com.konfigyr.namespace;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Service namespace services}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@Repository
public interface Services {

	/**
	 * Retrieve all {@link Service services} that are managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace for which services should be retrieved, can't be {@literal null}.
	 * @param query search query to be executed when searching for {@link Service services}, can't be {@literal null}.
	 * @return paged collections of services, never {@literal null}.
	 */
	Page<Service> find(Namespace namespace, SearchQuery query);

	/**
	 * Retrieve a single {@link Service} by its entity identifier.
	 *
	 * @param id service entity identifier, can't be {@literal null}.
	 * @return found service or an empty {@link Optional}, never {@literal null}.
	 */
	Optional<Service> get(EntityId id);

	/**
	 * Retrieve a single {@link Service} by its slug that is managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}
	 * @param slug      service slug, can't be {@literal null}
	 * @return found service or an empty {@link Optional}, never {@literal null}
	 */
	Optional<Service> get(Namespace namespace, String slug);

	/**
	 * Checks if a {@link Service} by its slug that is managed by the given {@link Namespace} exists.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}
	 * @param slug      service slug, can't be {@literal null}
	 * @return {@literal true} if there is a service with this slug, {@literal false} otherwise.
	 */
	boolean exists(Namespace namespace, String slug);

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
	@DomainEventPublisher(publishes = "namespaces.service-created")
	Service create(ServiceDefinition definition);

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
	Service update(EntityId id, ServiceDefinition definition);

	/**
	 * Returns the current manifest associated with the given service.
	 * <p>
	 * A manifest represents the set of {@link ArtifactCoordinates artifacts} that are currently
	 * used by a specific service within a {@link Namespace}. Each artifact referenced by the
	 * manifest contributes configuration metadata resolved through the {@code Artifactory} domain.
	 * <p>
	 * The manifest acts as the bridge between a service and the configuration metadata
	 * provided by its dependencies. When a manifest is resolved, the Artifactory aggregates
	 * the configuration property definitions contributed by all referenced artifacts and
	 * produces the effective configuration metadata used by the service.
	 *
	 * @param service service for which manifest should be retrieved, can't be {@literal null}
	 * @return the current {@link Manifest} for the service, never {@literal null}
	 */
	Manifest manifest(Service service);

	/**
	 * Returns the complete configuration catalog of the specified {@link Service}
	 * with the given entity identifier.
	 * <p>
	 * The returned {@link ServiceCatalog} represents the materialized set of configuration
	 * property descriptors available to the service. The catalog is derived from the
	 * service's {@link Manifest} and contains property definitions contributed by all
	 * artifacts that participate in the service release.
	 * <p>
	 * Consumers may use the catalog to perform local discovery of configuration properties,
	 * implement autocomplete functionality, or merge property descriptors with the current
	 * configuration state.
	 *
	 * @param id the service identifier for which to retrieve the catalog, can't be {@literal null}
	 * @return the configuration catalog of the service; never {@literal null}
	 * @see ServiceCatalog
	 */
	ServiceCatalog catalog(EntityId id);

	/**
	 * Returns the complete configuration catalog of the specified {@link Service}
	 * slug that is managed by the given {@link Namespace}
	 * <p>
	 * The returned {@link ServiceCatalog} represents the materialized set of configuration
	 * property descriptors available to the service. The catalog is derived from the
	 * service's {@link Manifest} and contains property definitions contributed by all
	 * artifacts that participate in the service release.
	 * <p>
	 * Consumers may use the catalog to perform local discovery of configuration properties,
	 * implement autocomplete functionality, or merge property descriptors with the current
	 * configuration state.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}
	 * @param slug the service slug for which to retrieve the catalog, can't be {@literal null}
	 * @return the configuration catalog of the service; never {@literal null}
	 * @see ServiceCatalog
	 */
	ServiceCatalog catalog(Namespace namespace, String slug);

	/**
	 * Searches the configuration catalog of the specified {@link Service} and returns a page of
	 * {@link PropertyDescriptor property descriptors} matching the provided {@link SearchQuery}.
	 * <p>
	 * The search operates exclusively within the service's configuration catalog and returns
	 * descriptors originating from artifacts referenced in the service's current {@link Manifest}.
	 *
	 * @param service configuration catalog owner, must not be {@literal null}
	 * @param query the search query describing filtering and pagination parameters, must not be {@literal null}
	 * @return descriptors that match the query; never {@literal null} but may be empty
	 * @see ServiceCatalog
	 */
	Page<PropertyDescriptor> search(Service service, SearchQuery query);

	/**
	 * Updates the manifest of a service with a new set of artifact dependencies.
	 * <p>
	 * The provided collection represents the complete set of artifacts that should be associated with the
	 * service after the update. Each artifact is identified using its Maven coordinates ({@code groupId},
	 * {@code artifactId}, {@code version}).
	 * <p>
	 * During this process the system performs several operations:
	 * <ul>
	 *     <li>Validates that each referenced artifact exists in the Artifactory</li>
	 *     <li>Resolves configuration metadata contributed by the artifacts</li>
	 *     <li>Computes the effective property definitions used by the service</li>
	 *     <li>Updates the service manifest to reflect the new dependency set</li>
	 * </ul>
	 *
	 * @param service service for which release should be created, can't be {@literal null}
	 * @param artifacts the complete collection of artifacts that should compose the
	 *                  service manifest, never {@literal null} but may be empty
	 * @return the updated {@link Manifest} reflecting the resolved dependency set
	 */
	Manifest publish(Service service, Collection<? extends ArtifactCoordinates> artifacts);

	/**
	 * Deletes a single {@link Service} by its entity identifier.
	 *
	 * @param id service entity identifier, can't be {@literal null}.
	 * @throws ServiceNotFoundException when a {@link Service} with the given entity identifier does not exist.
	 */
	@DomainEventPublisher(publishes = "namespace.service-deleted")
	void delete(EntityId id);

	/**
	 * Deletes a single {@link Service} by its slug that is managed by the given {@link Namespace}.
	 *
	 * @param namespace namespace that owns the service, can't be {@literal null}.
	 * @param slug      service slug, can't be {@literal null}.
	 * @throws ServiceNotFoundException when a {@link Service} with the given slug does not exist
	 * within the given {@link Namespace}.
	 */
	@DomainEventPublisher(publishes = "namespace.service-deleted")
	void delete(Namespace namespace, String slug);

}
