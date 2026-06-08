package com.konfigyr.mcp;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.AccessDeniedException;

/**
 * Resolves the target {@link Namespace} for the current MCP tool invocation from
 * the active {@link AuthenticatedPrincipal} without requiring the namespace to be
 * specified as an explicit MCP tool parameter.
 * <p>
 * Resolution only works if the <b>{@link PrincipalType#OAUTH_CLIENT}</b> principal
 * type is in the context. The JWT {@code sub} claim carries the OAuth2 {@code client_id}
 * of a {@link com.konfigyr.namespace.NamespaceApplication}. Because each application is
 * scoped to exactly one namespace, the namespace is implicit and resolved automatically via
 * {@link com.konfigyr.namespace.NamespaceManager#findNamespaceByClientId(String)}.
 * <p>
 * Each HTTP request sent to the Konfigyr MCP server is a distinct HTTP POST under the
 * stateless streamable transport, so the request-scoped implementation performs a single
 * DB lookup per request and caches the result within that request scope.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see McpNamespaceContext
 */
@NullMarked
public interface NamespaceContext {

	/**
	 * Resolves the {@link Namespace} associated with the currently authenticated principal.
	 * <p>
	 * Designed for tools that are exclusively invoked by {@link PrincipalType#OAUTH_CLIENT}
	 * service accounts. Because each namespace application is scoped to exactly one namespace,
	 * no additional input from the MCP client is required.
	 *
	 * @return the resolved namespace, never {@literal null}
	 * @throws AccessDeniedException if the namespace cannot be determined from the current
	 * authentication, for example, when the caller is a user account or when no active application
	 * matches the JWT subject
	 */
	Namespace resolve();

}
