package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
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

}
