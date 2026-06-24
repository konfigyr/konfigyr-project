package com.konfigyr.security;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * Represents an authenticated principal that may be scoped to a namespace.
 * <p>
 * This interface marks principals that have the capability to carry a namespace identifier.
 * Not all principals have a namespace context. For example:
 * <ul>
 * 	<li>OAuth principals may have a namespace if it was provided via JWT claims</li>
 *  <li>Service accounts may operate across multiple namespaces or none</li>
 * </ul>
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@NullMarked
public interface NamespacedPrincipal {

	/**
	 * Returns the namespace identifier associated with the principal.
	 *
	 * @return the namespace identifier, or {@code empty} if this principal is not bound to a namespace
	 */
	Optional<EntityId> getNamespaceId();

}
