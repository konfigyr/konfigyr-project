package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.*;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service that defines how {@link Profile profiles} are created and managed within the {@code Vault}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface ProfileManager {

	/**
	 * Retrieve all {@link Profile profiles} that are managed by the given {@link Service}.
	 *
	 * @param service service for which configuration profile should be retrieved, can't be {@literal null}.
	 * @param query search query to be executed when searching for {@link Profile profiles}, can't be {@literal null}.
	 * @return paged collections of profiles, never {@literal null}.
	 */
	Page<Profile> find(Service service, SearchQuery query);

	/**
	 * Retrieve a single {@link Profile} by its unique identifier.
	 *
	 * @param id profile entity identifier, can't be {@literal null}.
	 * @return found profile or an empty {@link Optional}, never {@literal null}
	 */
	Optional<Profile> get(EntityId id);

	/**
	 * Retrieve a single {@link Profile} by its slug that is managed by the given {@link Service}.
	 *
	 * @param service service that owns the profile, can't be {@literal null}
	 * @param slug    profile slug, can't be {@literal null}
	 * @return found profile or an empty {@link Optional}, never {@literal null}
	 */
	Optional<Profile> get(Service service, String slug);

	/**
	 * Checks if a {@link Profile} by its slug that is managed by the given {@link Service} exists.
	 *
	 * @param service service that owns the profile, can't be {@literal null}
	 * @param slug    profile slug, can't be {@literal null}
	 * @return {@literal true} if there is a profile with this slug, {@literal false} otherwise.
	 */
	boolean exists(Service service, String slug);

	/**
	 * Creates a new {@link Profile} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link ServiceEvent.Created}
	 * when a {@link Profile} was successfully created.
	 *
	 * @param definition definition used to create the profile, can't be {@literal null}
	 * @return created profile, never {@literal null}
	 * @throws ServiceExistsException when there is already a {@link Service} with the same slug
	 * @throws NamespaceNotFoundException when a {@link Namespace} does not exist
	 */
	@DomainEventPublisher(publishes = "vault.profile-created")
	Profile create(ProfileDefinition definition);

	/**
	 * Updates an existing {@link Profile} using the given definition.
	 * <p>
	 * The implementations of this interface should publish an {@link NamespaceEvent.Renamed}
	 * when a {@link Profile} was successfully updated.
	 *
	 * @param id profile entity identifier, can't be {@literal null}
	 * @param definition definition used to update the service, can't be {@literal null}
	 * @return updated profile, never {@literal null}
	 * @throws ServiceExistsException when there is already a {@link Profile} with the same slug
	 * @throws ServiceNotFoundException when a {@link Profile} with the given identifier does not exist
	 * @throws NamespaceNotFoundException when a {@link Namespace} does not exist
	 */
	@DomainEventPublisher(publishes = "vault.profile-updated")
	Profile update(EntityId id, ProfileDefinition definition);

	/**
	 * Deletes a single {@link Service} by its entity identifier.
	 *
	 * @param id service entity identifier, can't be {@literal null}.
	 * @throws ServiceNotFoundException when a {@link Service} with the given entity identifier does not exist.
	 */
	@DomainEventPublisher(publishes = "vault.profile-deleted")
	void delete(EntityId id);

	/**
	 * Deletes a single {@link Profile} by its slug that is managed by the given {@link Service}.
	 *
	 * @param service service that owns the profile, can't be {@literal null}
	 * @param slug    profile slug, can't be {@literal null}
	 * @throws ServiceNotFoundException when a {@link Profile} with the given slug does not exist
	 * within the given {@link Service}.
	 */
	@DomainEventPublisher(publishes = "vault.profile-deleted")
	void delete(Service service, String slug);

}
