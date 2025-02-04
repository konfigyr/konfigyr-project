package com.konfigyr.security.access;

import com.konfigyr.account.Memberships;
import com.konfigyr.namespace.Namespace;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/**
 * Konfigyr interface for expression root objects used with expression-based security that would additionally
 * be able to check {@link Namespace} access rights via {@link Memberships} that are present on the
 * {@link Authentication} object.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Memberships
 * @see SecurityExpressionOperations
 **/
public interface KonfigyrSecurityExpressionOperations extends SecurityExpressionOperations {

	/**
	 * Checks if the current {@link java.security.Principal} is a member of this {@link Namespace}.
	 *
	 * @param namespace slug of the namespace that the current principal should be a member of, can't be {@literal null}
	 * @return {@code true} when the current principal is a namespace member.
	 */
	default boolean isMember(@NonNull Namespace namespace) {
		return isMember(namespace.slug());
	}

	/**
	 * Checks if the current {@link java.security.Principal} is a member of a {@link Namespace}
	 * with the matching slug.
	 *
	 * @param namespace slug of the namespace that the current principal should be a member of, can't be {@literal null}
	 * @return {@code true} when the current principal is a namespace member.
	 */
	boolean isMember(@NonNull String namespace);

	/**
	 * Checks if the current {@link java.security.Principal} is an administrative member of this {@link Namespace}.
	 *
	 * @param namespace slug of the namespace that the current principal should be an admin of, can't be {@literal null}
	 * @return {@code true} when the current principal is a namespace administrator.
	 */
	default boolean isAdmin(@NonNull Namespace namespace) {
		return isAdmin(namespace.slug());
	}

	/**
	 * Checks if the current {@link java.security.Principal} is an administrative member of a
	 * {@link Namespace} with the matching slug.
	 *
	 * @param namespace slug of the namespace that the current principal should be an admin of, can't be {@literal null}
	 * @return {@code true} when the current principal is a namespace administrator.
	 */
	boolean isAdmin(@NonNull String namespace);

}
