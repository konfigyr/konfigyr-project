package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
	 * Retrieves all {@link Member members} of a given {@link Namespace} within a single page.
	 *
	 * @param id namespace entity identifier for which to fetch members, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(EntityId id) {
		return findMembers(id, Pageable.unpaged());
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param id namespace entity identifier for which to fetch members, can't be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(EntityId id, Pageable pageable) {
		return findMembers(id, SearchQuery.of(pageable));
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param id namespace entity identifier for which to fetch members, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	Page<Member> findMembers(EntityId id, SearchQuery query);

	/**
	 * Retrieves all {@link Member members} of a given {@link Namespace} within a single page.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(String slug) {
		return findMembers(slug, Pageable.unpaged());
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(String slug, Pageable pageable) {
		return findMembers(slug, SearchQuery.of(pageable));
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	Page<Member> findMembers(String slug, SearchQuery query);

	/**
	 * Retrieves all {@link Member members} of a given {@link Namespace} within a single page.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(Namespace namespace) {
		return findMembers(namespace, Pageable.unpaged());
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	default Page<Member> findMembers(Namespace namespace, Pageable pageable) {
		return findMembers(namespace, SearchQuery.of(pageable));
	}

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	Page<Member> findMembers(Namespace namespace, SearchQuery query);

	/**
	 * Retrieves the {@link Member} with given entity identifier in the {@link Namespace} team.
	 *
	 * @param member entity identifier of the {@link Member} to be retrieved, can't be {@literal null}
	 * @return the matching member or an empty {@link Optional}, never {@literal null}
	 */
	Optional<Member> getMember(EntityId member);

	/**
	 * Updates the {@link NamespaceRole} of the {@link Member} with given entity identifier
	 * in the {@link Namespace} team.
	 *
	 * @param member entity identifier of the {@link Member} to be removed, can't be {@literal null}
	 * @param role the new {@link NamespaceRole} that should be assigned, can't be {@literal null}
	 * @return the update member, never {@literal null}
	 */
	Member updateMember(EntityId member, NamespaceRole role);

	/**
	 * Removes the {@link Member} with given entity identifier from the {@link Namespace} team.
	 *
	 * @param member entity identifier of the {@link Member} to be removed, can't be {@literal null}
	 */
	void removeMember(EntityId member);

	/**
	 * Retrieves a page of {@link NamespaceApplication namespace applications} that are matching the
	 * criteria specified by the {@link SearchQuery}.
	 *
	 * @param query search query to be executed when searching for applications, can't be {@literal null}
	 * @return namespace applications, never {@literal null}
	 */
	Page<NamespaceApplication> findApplications(SearchQuery query);

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
	 * @param definition definition used to create the application for a namespace, can't be {@literal null}
	 * @return created namespace application, never {@literal null}
	 */
	NamespaceApplication createApplication(NamespaceApplicationDefinition definition);

	/**
	 * Updates the {@link NamespaceApplication} with given entity identifier in the {@link Namespace} with
	 * the data specified in the {@link NamespaceApplicationDefinition}.
	 * <p>
	 * Generates a new {@code client_secret} for the {@link NamespaceApplication} with the given entity identifier.
	 *
	 * @param application identifier of the {@link NamespaceApplication} to be updated, can't be {@literal null}
	 * @param definition the new definition to be applied to the application, can't be {@literal null}
	 * @return updated namespace application, never {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	NamespaceApplication updateApplication(EntityId application, NamespaceApplicationDefinition definition);

	/**
	 * Generates a new {@code client_secret} for the {@link NamespaceApplication} with the given entity identifier.
	 *
	 * @param application identifier of the {@link NamespaceApplication} to be reset, can't be {@literal null}
	 * @return updated namespace application, never {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	NamespaceApplication resetApplication(EntityId application);

	/**
	 * Removes the {@link NamespaceApplication} with given entity identifier from the {@link Namespace}.
	 *
	 * @param application entity identifier of the {@link NamespaceApplication} to be removed, can't be {@literal null}
	 * @throws NamespaceApplicationNotFoundException when an application does not exist
	 */
	void removeApplication(EntityId application);

}
