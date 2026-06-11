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
	 * Prefer {@link #WORKLOAD} for any environment that can issue OIDC tokens, as
	 * workload identity federation eliminates the operational burden of secret rotation.
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
	 * <strong>Workload Identity</strong>: for any workload that can obtain a
	 * platform-issued OIDC token as proof of identity — CI/CD pipelines (GitHub
	 * Actions, GitLab CI, CircleCI), cloud runtimes (AWS Lambda, GCP Cloud Run,
	 * Azure Container Apps), Kubernetes pods, and build tooling.
	 * <p>
	 * Uses <em>Workload Identity Federation</em> via the token exchange grant (RFC 8693).
	 * The workload presents its OIDC token (e.g. from
	 * {@code token.actions.githubusercontent.com}), which the authorization server
	 * validates against a configured trusted issuer and exchanges for a scoped
	 * Konfigyr access token. No client secret is required or generated.
	 * <p>
	 * Prefer this type over {@link #SERVICE_ACCOUNT} for any environment that can
	 * issue OIDC tokens, as the external token replaces the need for a static,
	 * long-lived credential.
	 */
	WORKLOAD((byte) 0x03, "Workload Identity");

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
	 * Only {@link #SERVICE_ACCOUNT} authenticates via a shared secret
	 * ({@code client_credentials} grant). {@link #AGENT} and {@link #WORKLOAD} are public
	 * clients whose {@code client_secret} is always {@literal null} in the database:
	 * {@link #AGENT} runs on a user's device where a secret cannot be stored securely, and
	 * {@link #WORKLOAD} relies on a cryptographically signed external OIDC token as its
	 * sole credential during token exchange.
	 *
	 * @return {@code true} if a client secret must be generated for this type
	 */
	public boolean requiresSecret() {
		return this == SERVICE_ACCOUNT;
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
