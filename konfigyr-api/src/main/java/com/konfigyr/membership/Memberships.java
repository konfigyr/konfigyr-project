package com.konfigyr.membership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when managing {@link Member namespace members}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@Repository
public interface Memberships {

	/**
	 * Returns all {@link Member members} of a given {@link Namespace}.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @return namespace members page, never {@literal null}
	 */
	default Page<Member> find(Namespace namespace) {
		return find(namespace, Pageable.unpaged());
	}

	/**
	 * Returns a page of {@link Member members} of a given {@link Namespace}.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}
	 * @return namespace members page, never {@literal null}
	 */
	default Page<Member> find(Namespace namespace, Pageable pageable) {
		return find(namespace, SearchQuery.of(pageable));
	}

	/**
	 * Returns a page of {@link Member members} of a given {@link Namespace} matching the search query.
	 *
	 * @param namespace namespace for which to fetch members, can't be {@literal null}
	 * @param query search query, can't be {@literal null}
	 * @return namespace members page, never {@literal null}
	 */
	Page<Member> find(Namespace namespace, SearchQuery query);

	/**
	 * Returns the {@link Member} with given entity identifier in the {@link Namespace} team.
	 *
	 * @param namespace namespace for which the member is retrieved, can't be {@literal null}
	 * @param id entity identifier of the {@link Member} to be retrieved, can't be {@literal null}
	 * @return the matching member or an empty {@link Optional}, never {@literal null}
	 */
	Optional<Member> get(Namespace namespace, EntityId id);

	/**
	 * Updates the {@link NamespaceRole} of the {@link Member} with the given entity identifier.
	 *
	 * @param namespace namespace for which the member is updated, can't be {@literal null}
	 * @param id entity identifier of the {@link Member} to be updated, can't be {@literal null}
	 * @param role the new {@link NamespaceRole} to assign, can't be {@literal null}
	 * @return the updated member, never {@literal null}
	 * @throws MemberNotFoundException when a {@link Member} with the given identifier does not exist
	 * @throws UnsupportedMembershipOperationException when the update would leave the namespace without an administrator
	 */
	@DomainEventPublisher(publishes = "namespaces.member-updated")
	Member update(Namespace namespace, EntityId id, NamespaceRole role);

	/**
	 * Removes the {@link Member} with the given entity identifier from the {@link Namespace} team.
	 *
	 * @param namespace namespace the member is being removed from, can't be {@literal null}
	 * @param id entity identifier of the {@link Member} to remove, can't be {@literal null}
	 * @throws MemberNotFoundException when a {@link Member} with the given identifier does not exist
	 * @throws UnsupportedMembershipOperationException when removing would leave the namespace without an administrator
	 */
	@DomainEventPublisher(publishes = "namespaces.member-removed")
	void remove(Namespace namespace, EntityId id);

}
