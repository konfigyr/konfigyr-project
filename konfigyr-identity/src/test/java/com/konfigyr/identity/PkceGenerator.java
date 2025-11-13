package com.konfigyr.identity;

import org.springframework.security.crypto.keygen.KeyGenerators;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class PkceGenerator {

	static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	private PkceGenerator() {
	}

	/**
	 * Generates a cryptographically random code verifier (43-128 characters).
	 * The verifier is a URL-safe Base64 encoded string from random bytes.
	 *
	 * @return The generated code verifier.
	 */
	public static String generateCodeVerifier() {
		final byte[] verifier = KeyGenerators.secureRandom(32).generateKey();
		return ENCODER.encodeToString(verifier);
	}

	/**
	 * Generates the code challenge from the code verifier using SHA-256 and Base64-URL encoding.
	 *
	 * @param verifier The code verifier string.
	 * @return The generated code challenge.
	 */
	public static String generateCodeChallenge(String verifier) {
		try {
			final byte[] bytes = verifier.getBytes(StandardCharsets.UTF_8);
			final MessageDigest md = MessageDigest.getInstance("SHA-256");

			return ENCODER.encodeToString(md.digest(bytes));
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to generate the code challenge for verifier: " + verifier, ex);
		}
	}

}
