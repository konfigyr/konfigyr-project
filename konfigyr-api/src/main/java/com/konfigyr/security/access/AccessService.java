package com.konfigyr.security.access;

import com.konfigyr.namespace.NamespaceRole;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

/**
 * Interface used to evaluate if the currently {@link Authentication} has sufficient permissions to
 * access a {@link com.konfigyr.namespace.Namespace}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface AccessService {

	/**
	 * Checks if the given {@link Authentication} has access to the {@link com.konfigyr.namespace.Namespace} with the
	 * given slug.
	 *
	 * @param authentication the current authentication object, can't be {@literal null}
	 * @param namespace the namespace slug to be accessed, can't be {@literal null}
	 * @return the authorization result or {@literal null} if no decision could be made
	 */
	@Nullable
	AuthorizationResult hasAccess(@NonNull Authentication authentication, @NonNull String namespace);

	/**
	 * Checks if the given {@link Authentication} has access to the {@link com.konfigyr.namespace.Namespace} with the
	 * given slug and {@link NamespaceRole}.
	 *
	 * @param authentication the current authentication object, can't be {@literal null}
	 * @param namespace the namespace slug to be accessed, can't be {@literal null}
	 * @param role the namespace role that current authentication should have, can't be {@literal null}
	 * @return the authorization result or {@literal null} if no decision could be made
	 */
	@Nullable
	AuthorizationResult hasAccess(@NonNull Authentication authentication, @NonNull String namespace, @NonNull NamespaceRole role);

}
