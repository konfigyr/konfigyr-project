package com.konfigyr.security;

/**
 * Classifies the intended purpose of a namespace OAuth application.
 * <p>
 * The type is encoded as a single byte inside every {@link NamespaceClientId}, which
 * means the client type can always be determined from the {@code client_id} string alone,
 * no database lookup required. Authorization server configuration and validation
 * logic use this to derive the correct grant types, client authentication methods, and
 * security constraints for each application.
 * <p>
 * Byte codes are stable across versions. Never reuse a code once it has been assigned,
 * and never rely on {@link Enum#ordinal()} for persistence.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceClientId
 */
public enum NamespaceClientType {

	/**
	 * <strong>Service Account</strong>: a machine identity with a long-lived client
	 * secret for backend services and scripts that cannot participate in an interactive
	 * or federated OAuth flow.
	 * <p>
	 * Uses the <em>Client Credentials</em> grant (RFC 6749 §4.4). The application
	 * authenticates directly with its {@code client_id} and {@code client_secret} and
	 * receives an access token scoped to the owning namespace.
	 * <p>
	 * Prefer {@link #PIPELINE} for CI/CD environments that support OIDC token
	 * issuance, as that eliminates the operational burden of secret rotation.
	 */
	SERVICE_ACCOUNT((byte) 0x01, "Service Account"),

	/**
	 * <strong>AI Agent</strong>: for AI agents and coding assistants that access
	 * namespace configuration on behalf of an authenticated, verified member
	 * (e.g. Claude Code, Claude Desktop, or any MCP-compatible tool).
	 * <p>
	 * Uses the <em>Authorization Code + PKCE</em> flow (RFC 7636). The agent opens a
	 * browser for the user to authenticate, receives an authorization code on a
	 * loopback redirect URI ({@code http://127.0.0.1}, any port), and exchanges it
	 * for a token. No client secret is required or generated. Namespace membership
	 * is verified before the authorization code is issued.
	 */
	AGENT((byte) 0x02, "AI Agent"),

	/**
	 * <strong>Pipeline Integration</strong>: for CI/CD pipelines and build tooling
	 * (GitHub Actions, GitLab CI, CircleCI, the Konfigyr Gradle and Maven plugins).
	 * <p>
	 * Uses <em>OIDC federation</em> via the token exchange grant (RFC 8693). The
	 * pipeline presents its own platform-issued OIDC token (e.g., from
	 * {@code token.actions.githubusercontent.com}), which the authorization server
	 * validates against a configured external issuer and exchanges for a scoped
	 * Konfigyr access token.
	 * <p>
	 * Unlike {@link #AGENT}, this is a <em>confidential client</em>: the CI runner
	 * operates on server-side infrastructure that can securely store a client secret.
	 * Requiring the secret alongside the OIDC token provides defense in depth;
	 * an intercepted {@code subject_token} alone is not enough to retrieve a
	 * Konfigyr token; and ensures every exchange is attributed to a specific,
	 * named pipeline application in the audit log.
	 * <p>
	 * Prefer this type over {@link #SERVICE_ACCOUNT} for any environment that can
	 * issue OIDC tokens, as the external token replaces the need for a static,
	 * long-lived credential while the client secret remains the identity anchor.
	 */
	PIPELINE((byte) 0x03, "Pipeline Integration");

	/** The stable single-byte encoding of this type embedded in every {@link NamespaceClientId}. */
	private final byte code;

	/** The human-readable label displayed in UIs and user-facing messages. */
	private final String displayName;

	NamespaceClientType(byte code, String displayName) {
		this.code = code;
		this.displayName = displayName;
	}

	/**
	 * Returns the human-readable label for this type, suitable for display in UIs and
	 * user-facing messages (e.g. {@code "AI Agent"}, {@code "Service Account"}).
	 * <p>
	 * Prefer this over {@link #name()} whenever the value is shown to an end user.
	 *
	 * @return display name, never {@literal null}
	 */
	public String displayName() {
		return displayName;
	}

	/**
	 * Returns {@code true} when this client type requires a client secret to be generated
	 * at creation time and presented with every token request.
	 * <p>
	 * {@link #SERVICE_ACCOUNT} and {@link #PIPELINE} are confidential clients that
	 * authenticate with a secret, {@link #SERVICE_ACCOUNT} exclusively via
	 * {@code client_credentials}, and {@link #PIPELINE} as a second factor alongside the
	 * external OIDC token in the token exchange flow. Requiring a secret for
	 * {@link #PIPELINE} prevents an intercepted {@code subject_token} from being
	 * redeemed by an attacker and ensures each exchange is attributed to a specific
	 * named application in the audit log.
	 * <p>
	 * Only {@link #AGENT} returns {@code false}, it is a true public client running on
	 * a user's device where a secret cannot be stored securely. Its {@code client_secret}
	 * is always {@literal null} in the database.
	 *
	 * @return {@code true} if a client secret must be generated for this type
	 */
	public boolean requiresSecret() {
		return this == SERVICE_ACCOUNT || this == PIPELINE;
	}

	/**
	 * Returns the single-byte code that represents this type inside a
	 * {@link NamespaceClientId} payload. Only {@link NamespaceClientId} should
	 * use this when encoding or decoding a client ID, external callers work
	 * with the enum constant directly.
	 *
	 * @return stable byte code for this type
	 */
	byte code() {
		return code;
	}

	/**
	 * Resolves a {@link NamespaceClientType} from its encoded byte. Only
	 * {@link NamespaceClientId} should call this during {@code parse()}, external
	 * callers have no need to work with raw type bytes.
	 *
	 * @param code the byte read from a {@link NamespaceClientId} payload
	 * @return matching type, never {@literal null}
	 * @throws IllegalArgumentException when no type is registered for the given code
	 */
	static NamespaceClientType of(byte code) {
		for (final NamespaceClientType type : values()) {
			if (type.code == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown namespace client type code: " + code);
	}

}
