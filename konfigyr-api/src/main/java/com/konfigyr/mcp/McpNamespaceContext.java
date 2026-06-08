package com.konfigyr.mcp;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Request-scoped {@link NamespaceContext} implementation that resolves the target
 * {@link Namespace} from the JWT {@code sub} claim of an {@link PrincipalType#OAUTH_CLIENT}
 * principal.
 * <p>
 * The stateless streamable MCP transport maps each request to Konfigyr MCP server to its
 * own HTTP POST request, so {@link org.springframework.web.context.request.RequestScope}
 * guarantees that resolution happens once per tool invocation and the result is cached for
 * the lifetime of that request, no repeated database round-trips within a single MCP operation.
 * <p>
 * Resolution steps:
 * <ol>
 *   <li>
 *       Read the {@link AuthenticatedPrincipal} from {@link org.springframework.security.core.context.SecurityContextHolder}
 *   </li>
 *   <li>
 *       Guard that the principal type is {@link PrincipalType#OAUTH_CLIENT}, any other types
 *       cannot be resolved automatically and will throw {@link AccessDeniedException}
 *   </li>
 *   <li>
 *       Use the principal's stable identifier (JWT {@code sub}) as the {@code client_id} and
 *       delegate to {@link NamespaceManager#findNamespaceByClientId(String)}
 *   </li>
 *   <li>
 *       Throw {@link AccessDeniedException} if no matching application (and therefore no
 *       namespace) exists for that {@code client_id}
 *   </li>
 * </ol>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@RequiredArgsConstructor
class McpNamespaceContext implements NamespaceContext {

	private final NamespaceManager namespaces;

	/**
	 * Cached within the HTTP request scope, resolved at most once per MCP server request.
	 */
	@Nullable
	private Namespace resolved;

	@NonNull
	@Override
	public Namespace resolve() {
		if (resolved != null) {
			return resolved;
		}

		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

		if (principal.getType() != PrincipalType.OAUTH_CLIENT) {
			throw new AccessDeniedException("Namespace cannot be resolved for '%s' principal type".formatted(principal.getType()));
		}

		resolved = namespaces.findNamespaceByClientId(principal.get()).orElseThrow(() -> new AccessDeniedException(
				"No active namespace application found for client: '%s'".formatted(principal.get())
		));

		return resolved;
	}

}
