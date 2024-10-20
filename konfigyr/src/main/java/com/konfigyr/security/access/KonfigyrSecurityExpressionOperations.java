package com.konfigyr.security.access;

import com.konfigyr.account.Membership;
import com.konfigyr.account.Memberships;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.security.AccountPrincipal;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.function.Predicate;

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
	default boolean isMember(@NonNull String namespace) {
		return getMemberships().get().anyMatch(matchesNamespace(namespace));
	}

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
	default boolean isAdmin(@NonNull String namespace) {
		return getMemberships().get().anyMatch(
				matchesNamespace(namespace).and(matchesRole(NamespaceRole.ADMIN))
		);
	}

	/**
	 * Gets the {@link Memberships} used for evaluating the expressions from the current {@link Authentication}
	 * object if present.
	 *
	 * @return the {@link Memberships} for evaluating the expressions.
	 */
	@NonNull
	default Memberships getMemberships() {
		final Authentication authentication = getAuthentication();

		if (authentication == null) {
			return Memberships.empty();
		}

		if (authentication.getPrincipal() instanceof AccountPrincipal principal) {
			return principal.getMemberships();
		}
		return Memberships.empty();
	}

	static Predicate<Membership> matchesNamespace(@NonNull String namespace) {
		return membership -> namespace.equals(membership.namespace());
	}

	static Predicate<Membership> matchesRole(@NonNull NamespaceRole role) {
		return membership -> role.equals(membership.role());
	}

}
