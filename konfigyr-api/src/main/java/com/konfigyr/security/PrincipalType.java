package com.konfigyr.security;

/**
 * Represents the classification of an {@link AuthenticatedPrincipal} interacting with the Konfigyr API.
 * <p>
 * {@link PrincipalType} differentiates between various categories of authenticated actors. While
 * {@link AuthenticatedPrincipal} identifies <em>who</em> is making a request, this enum describes
 * <em>what kind</em> of an actor it is.
 * <p>
 * In software systems, not all callers are human users. Operations may originate from:
 * <ul>
 *     <li>Interactive human users via the UI</li>
 *     <li>Automated deployment pipelines</li>
 *     <li>Background system processes</li>
 *     <li>Internal platform services</li>
 * </ul>
 * These actors may require different:
 * <ul>
 *     <li>Authorization policies</li>
 *     <li>Audit handling rules</li>
 *     <li>Approval workflows</li>
 *     <li>Rate limits</li>
 *     <li>Operational restrictions</li>
 * </ul>
 * Explicitly modeling the principal classification prevents implicit assumptions in business
 * logic and makes security decisions intentional and transparent.
 * <p>
 * The value of this enum must originate from trusted authentication or system configuration.
 * It must never be derived from user-controlled input. This enum is deliberately independent of
 * Spring Security roles, OAuth scopes, or JWT claims. It represents a domain-level abstraction of
 * caller classification within the Vault boundary.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see AuthenticatedPrincipal
 */
public enum PrincipalType {

	/**
	 * A human user interacting with the API via UI or direct HTTP requests.
	 * <p>
	 * Typically authenticated through an external identity provider. This type represents interactive usage.
	 */
	USER_ACCOUNT,

	/**
	 * A non-human service account, most likely a {@link com.konfigyr.namespace.NamespaceApplication}.
	 * <p>
	 * Represents automated systems such as CI/CD pipelines, deployment agents, or platform integrations.
	 */
	OAUTH_CLIENT,

	/**
	 * An internal system-level actor.
	 * <p>
	 * Used for platform-initiated operations such as background tasks, maintenance routines, or internal migrations.
	 */
	SYSTEM

}
