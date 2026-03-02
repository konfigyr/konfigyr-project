package com.konfigyr.security;

import com.konfigyr.security.access.SecurityIdentity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Represents an authenticated actor interacting with the Konfigyr API. It is a lightweight,
 * immutable snapshot of the authenticated actor at the time an operation is performed.
 * <p>
 * {@link AuthenticatedPrincipal} is a cross-domain security abstraction that captures the identity
 * of the caller after successful authentication. It is intentionally independent of any authentication
 * method and does not represent a persisted user account.
 * <p>
 * This type exists to establish a clear security boundary between authentication infrastructure and
 * our domain logic. Konfigur modules (Accounts, Namespaces, Vault, etc.) must not depend on
 * framework-specific types such as {@code Authentication}. Instead, they operate on this neutral
 * abstraction.
 * <p>
 * Audit and traceability operations should be associated with an {@link AuthenticatedPrincipal}
 * when this actor is performing state mutations or sensitive operations within Konfigyr to ensure:
 * <ul>
 *     <li>Traceability of configuration changes</li>
 *     <li>Clear audit attribution</li>
 *     <li>Security review capabilities</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface AuthenticatedPrincipal extends SecurityIdentity {

	/**
	 * Returns the stable unique identifier of the authenticated actor.
	 * <p>
	 * Typically derived from the authentication provider's subject, in our case the JWT {@code sub} claim.
	 * This value must remain stable for the lifetime of the identity.
	 *
	 * @return the JWT {@code sub} claim that identifies the authenticated actor. Never {@literal null}.
	 */
	@Override
	String get();

	/**
	 * The classification of the actor (e.g., human user, service account).
	 * <p>
	 * Used to influence authorization rules and audit behavior.
	 *
	 * @return the principal type, never {@literal null}.
	 */
	PrincipalType getType();

	/**
	 * The email address of the actor that is used for audit readability.
	 *
	 * @return the email address, may be {@code empty} if not provided by the identity provider.
	 */
	Optional<@Nullable String> getEmail();

	/**
	 * A human-friendly display name. Intended strictly for presentation purposes.
	 * Must not be used as a unique identifier.
	 *
	 * @return the display name of the actor, may be {@code empty} if not provided by the identity provider.
	 */
	Optional<@Nullable String> getDisplayName();

	/**
	 * Attempts to resolve the current {@link AuthenticatedPrincipal} from the current {@link SecurityContextHolder}.
	 * <p>
	 * This method would retrieve the current {@link Authentication} from the security context, extract the
	 * principal if it is of type {@link AuthenticatedPrincipal}, return an empty {@link Optional} if no
	 * suitable principal is found.
	 * <p>
	 * No exception is thrown if authentication is missing or incompatible.
	 *
	 * @return the resolved principal, or {@code empty} if not available.
	 */
	static Optional<AuthenticatedPrincipal> fromSecurityContext() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
				.flatMap(AuthenticatedPrincipal::fromAuthentication);
	}

	/**
	 * Attempts to extract an {@link AuthenticatedPrincipal} from the given {@link Authentication} instance.
	 * <p>
	 * If the authentication's principal is already of type {@link AuthenticatedPrincipal}, it is returned.
	 * Otherwise, an empty {@link Optional} is returned.
	 *
	 * @param authentication the authentication object, may not be {@literal null}
	 * @return the resolved principal, or {@code empty} if not available.
	 */
	static Optional<AuthenticatedPrincipal> fromAuthentication(Authentication authentication) {
		if (authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
			return Optional.of(principal);
		}
		return Optional.empty();
	}

	/**
	 * Resolves the current {@link AuthenticatedPrincipal} from the security context.
	 * <p>
	 * Unlike {@link #fromSecurityContext()}, this method enforces presence of an authenticated principal and
	 * throws an {@link org.springframework.security.core.AuthenticationException} if none is found.
	 * <p>
	 * This method is suitable for endpoints that require authentication and should fail fast if the security
	 * context is missing or misconfigured.
	 *
	 * @return the authenticated principal, never {@literal null}
	 * @throws AuthenticationCredentialsNotFoundException if no authenticated principal is available
	 */
	static AuthenticatedPrincipal resolve() {
		return fromSecurityContext().orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
				"Could not find authenticated principal in the current security context."
		));
	}

}
