package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Namespace namespaces}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Repository
public interface NamespaceManager {

	/**
	 * Returns a {@link Namespace} with the given {@link EntityId identifier}. If the {@link Namespace}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param id namespace entity identifier, can't be {@literal null}
	 * @return matching namespace or empty, never {@literal null}
	 */
	@NonNull
	Optional<Namespace> findById(@NonNull EntityId id);

	/**
	 * Returns a {@link Namespace} with the given slug - (URL path). If the {@link Namespace}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @return matching namespace or empty, never {@literal null}
	 */
	@NonNull
	Optional<Namespace> findBySlug(@NonNull String slug);

	/**
	 * Checks if a {@link Namespace} with the given slug - (URL path) exists.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @return {@literal true} if there is namespace with this slug, {@literal false} otherwise.
	 */
	boolean exists(@NonNull String slug);

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
	@NonNull
	@DomainEventPublisher(publishes = "namespaces.created")
	Namespace create(@NonNull NamespaceDefinition definition);

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param id namespace entity identifier for which to fetch members, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	@NonNull
	Page<Member> findMembers(@NonNull EntityId id, @NonNull SearchQuery query);

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param slug namespace slug, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	@NonNull
	Page<Member> findMembers(@NonNull String slug, @NonNull SearchQuery query);

	/**
	 * Retrieves a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @param query search query to be executed when searching for {@link Member members}, can't be {@literal null}
	 * @return namespace members, never {@literal null}
	 */
	@NonNull
	Page<Member> findMembers(@NonNull Namespace namespace, @NonNull SearchQuery query);

	/**
	 * Updates the {@link NamespaceRole} of the {@link Member} with given entity identifier
	 * in the {@link Namespace} team.
	 *
	 * @param member entity identifier of the {@link Member} to be removed, can't be {@literal null}
	 * @param role the new {@link NamespaceRole} that should be assigned, can't be {@literal null}
	 * @return the update member, never {@literal null}
	 */
	@NonNull
	Member updateMember(@NonNull EntityId member, @NonNull NamespaceRole role);

	/**
	 * Removes the {@link Member} with given entity identifier from the {@link Namespace} team.
	 *
	 * @param member entity identifier of the {@link Member} to be removed, can't be {@literal null}
	 */
	void removeMember(@NonNull EntityId member);

}
