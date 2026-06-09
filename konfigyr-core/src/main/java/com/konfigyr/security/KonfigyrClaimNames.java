package com.konfigyr.security;

/**
 * Constants for JWT claim names that are specific to the Konfigyr platform.
 * <p>
 * These are private claims within the Konfigyr ecosystem and are not registered
 * in the IANA JWT Claims Registry. All consumers of Konfigyr-issued tokens should
 * reference these constants rather than inlining the string literals.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see <a href="https://www.iana.org/assignments/jwt/jwt.xhtml">IANA JWT Claims Registry</a>
 */
public final class KonfigyrClaimNames {

	/**
	 * The serialized {@link com.konfigyr.entity.EntityId} of the namespace that owns
	 * the OAuth client for which the token was issued. Present only on tokens issued
	 * to namespace applications (clients whose {@code client_id} starts with {@code kfg-}).
	 */
	public static final String NAMESPACE = "namespace";

	private KonfigyrClaimNames() {
	}
}
