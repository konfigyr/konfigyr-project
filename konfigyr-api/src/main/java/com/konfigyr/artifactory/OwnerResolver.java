package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;

/**
 * SPI for resolving a namespace into an {@link Owner} reference.
 * <p>
 * Callers hold a namespace identity either as a slug (e.g. from a URL path segment) or as an
 * {@link EntityId} (e.g. from a JWT claim). This interface covers both. Implementations should
 * be backed by the namespace tenant registry and should cache results.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 * @see Owner
 */
@NullMarked
public interface OwnerResolver {

	/**
	 * Resolves an {@link Owner} by slug.
	 *
	 * @param slug the owner slug to resolve
	 * @return the matching owner if one exists; otherwise an empty optional
	 */
	Owner resolve(String slug);

	/**
	 * Resolves an {@link Owner} by entity identifier.
	 *
	 * @param id the owner entity identifier to resolve
	 * @return the matching owner if one exists; otherwise an empty optional
	 */
	Owner resolve(EntityId id);

}
