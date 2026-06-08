package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Namespace namespaces}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@Repository
public interface NamespaceManager {

	/**
	 * Performs a lookup of available {@link Namespace namespaces} that are matching the
	 * filter {@link SearchQuery} filter criteria.
	 *
	 * @param query search query, can't be {@literal null}
	 * @return matching namespaces page, never {@literal null}
	 */
	Page<Namespace> search(SearchQuery query);

	/**
	 * Returns a {@link Namespace} with the given {@link EntityId identifier}. If the {@link Namespace}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param id namespace entity identifier, can't be {@literal null}
	 * @return matching namespace or empty, never {@literal null}
	 */
	Optional<Namespace> findById(EntityId id);

	/**
	 * Returns a {@link Namespace} with the given slug - (URL path). If the {@link Namespace}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @return matching namespace or empty, never {@literal null}
	 */
	Optional<Namespace> findBySlug(String slug);

	/**
	 * Checks if a {@link Namespace} with the entity identifier exists.
	 *
	 * @param id namespace entity identifier, can't be {@literal null}
	 * @return {@literal true} if there is namespace with this identifier, {@literal false} otherwise.
	 */
	boolean exists(EntityId id);

	/**
	 * Checks if a {@link Namespace} with the given slug - (URL path) exists.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @return {@literal true} if there is namespace with this slug, {@literal false} otherwise.
	 */
	boolean exists(String slug);

	/**
	 * Creates a new {@link Namespace} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link NamespaceEvent.Created}
	 * when a {@link Namespace} was successfully created.
	 *
	 * @param definition definition used to create the namespace, can't be {@literal null}
	 * @return created namespace, never {@literal null}
	 * @throws NamespaceExistsException when there is already a {@link Namespace} with the same slug
	 */
	@DomainEventPublisher(publishes = "namespaces.created")
	Namespace create(NamespaceDefinition definition);

	/**
	 * Updates the new {@link Namespace} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link NamespaceEvent.Renamed}
	 * when a {@link Namespace} slug was successfully updated.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @param definition definition used to update the namespace, can't be {@literal null}
	 * @return updated namespace, never {@literal null}
	 * @throws NamespaceExistsException when there is already a {@link Namespace} with the same slug
	 * @throws NamespaceNotFoundException when a {@link Namespace} with the given slug does not exist
	 */
	@DomainEventPublisher(publishes = "namespaces.renamed")
	Namespace update(String slug, NamespaceDefinition definition);

	/**
	 * Deletes an existing {@link Namespace} with a given URL slug.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @throws NamespaceNotFoundException when a {@link Namespace} with the given slug does not exist
	 */
	void delete(String slug);

	/**
	 * Retrieves a page of {@link NamespaceApplication namespace applications} that are matching the
	 * criteria specified by the {@link SearchQuery}.
	 *
	 * @param query search query to be executed when searching for applications, can't be {@literal null}
	 * @return namespace applications, never {@literal null}
	 */
	Page<NamespaceApplication> findApplications(SearchQuery query);

	/**
	 * Retrieves the {@link Namespace} that owns the {@link NamespaceApplication} identified by the given
	 * OAuth2 {@code client_id}.
	 * <p>
	 * This method is the primary entry-point for resolving the operational namespace from a JWT
	 * {@code sub} claim whose value follows the Konfigyr {@code kfg-...} client-id format. It is
	 * intentionally scoped to the namespace (not the application) because callers, particularly
	 * MCP tools and security filters, need the namespace context, not the application metadata.
	 * <p>
	 * The lookup performs a single query that joins {@code oauth_applications} with {@code namespaces}
	 * on the foreign-key relationship, so no second round-trip is required.
	 *
	 * @param clientId the OAuth2 {@code client_id} of the application, can't be {@literal null}
	 * @return namespace that owns the matching application, or empty if no application with
	 *         the given {@code client_id} exists, never {@literal null}
	 */
	Optional<Namespace> findNamespaceByClientId(String clientId);

	/**
	 * Retrieves the {@link NamespaceApplication} with given entity identifier in the {@link Namespace}.
	 *
	 * @param application entity identifier of the {@link NamespaceApplication} to be retrieved, can't be {@literal null}
	 * @return the matching application or an empty {@link Optional}, never {@literal null}
	 */
	Optional<NamespaceApplication> getApplication(EntityId application);

	/**
	 * Creates a new {@link NamespaceApplication} using the given definition.
	 *
	 * @param namespace namespace for which the application is created, can't be {@literal null}
	 * @param definition definition used to create the application for a namespace, can't be {@literal null}
	 * @return created namespace application, never {@literal null}
	 */
	@DomainEventPublisher(publishes = "namespaces.application-created")
	NamespaceApplication createApplication(Namespace namespace, NamespaceApplicationDefinition definition);

	/**
	 * Updates the {@link NamespaceApplication} with given entity identifier in the {@link Namespace} with
	 * the data specified in the {@link NamespaceApplicationDefinition}.
	 * <p>
	 * Generates a new {@code client_secret} for the {@link NamespaceApplication} with the given entity identifier.
	 *
	 * @param namespace namespace for which the application is updated, can't be {@literal null}
	 * @param application identifier of the {@link NamespaceApplication} to be updated, can't be {@literal null}
	 * @param definition the new definition to be applied to the application, can't be {@literal null}
	 * @return updated namespace application, never {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	@DomainEventPublisher(publishes = "namespaces.application-updated")
	NamespaceApplication updateApplication(Namespace namespace, EntityId application, NamespaceApplicationDefinition definition);

	/**
	 * Generates a new {@code client_secret} for the {@link NamespaceApplication} with the given entity identifier.
	 *
	 * @param namespace namespace for which the application is updated, can't be {@literal null}
	 * @param application identifier of the {@link NamespaceApplication} to be reset, can't be {@literal null}
	 * @return updated namespace application, never {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	@DomainEventPublisher(publishes = "namespaces.application-reset")
	NamespaceApplication resetApplication(Namespace namespace, EntityId application);

	/**
	 * Removes the {@link NamespaceApplication} with given entity identifier from the {@link Namespace}.
	 *
	 * @param namespace namespace for which the application is removed from, can't be {@literal null}
	 * @param application entity identifier of the {@link NamespaceApplication} to be removed, can't be {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	@DomainEventPublisher(publishes = "namespaces.application-removed")
	void removeApplication(Namespace namespace, EntityId application);

}
