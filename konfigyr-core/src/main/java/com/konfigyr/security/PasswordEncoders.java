package com.konfigyr.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.function.SingletonSupplier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for creating {@link PasswordEncoder} instances.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PasswordEncoder
 * @see PasswordEncoderFactories
 * @see Argon2PasswordEncoder
 */
public final class PasswordEncoders {

	private static final Supplier<PasswordEncoder> argon = SingletonSupplier.of(
			() -> new Argon2PasswordEncoder(16, 32, 1, 19456, 2)
	);

	private static final Supplier<PasswordEncoder> delegate = SingletonSupplier.of(
			PasswordEncoders::createDelegatingPasswordEncoder
	);

	private PasswordEncoders() {
	}

	/**
	 * Creates a {@link PasswordEncoder} that would use the {@code argon2} algorithm to encode passwords.
	 * <p>
	 * This instance of the encoder would use the default {@link PasswordEncoder} that is created by the
	 * {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()} method to match passwords
	 * that are not encoded with the {@code argon2} algorithm.
	 *
	 * @return the {@link PasswordEncoder} to be used across the Konfigyr application, never {@literal null}.
	 * @see #argon()
	 * @see PasswordEncoderFactories#createDelegatingPasswordEncoder()
	 */
	@NonNull
	public static PasswordEncoder get() {
		return delegate.get();
	}

	/**
	 * Creates an {@link PasswordEncoder} using {@code argon2id} algorithm. The encoder is configured
	 * with the following parameters:
	 * <ul>
	 *     <li>Salt length of {@code 16 bytes} is the universally accepted standard and is more than sufficient.</li>
	 *     <li>Has length {@code 32 bytes} provides excellent security and collision resistance.</li>
	 *     <li>Iteration count is set to {@code 2}.</li>
	 *     <li>
	 *         Parallelism count is set to {@code 1}, which is common for a standard web server setup
	 *         to avoid resource exhaustion (Denial of Service concerns).
	 *     </li>
	 *     <li>Memory cost is set to {@code 19456} according to OWASP recommendation.</li>
	 * </ul>
	 *
	 * @return the Argon2 {@link PasswordEncoder} instance, never {@literal null}.
	 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">
	 *     OWASP Password Storage Cheat Sheet</a>
	 */
	@NonNull
	public static PasswordEncoder argon() {
		return argon.get();
	}

	private static PasswordEncoder createDelegatingPasswordEncoder() {
		// this registration is used by the deprecated Argon2PasswordEncoder, but it should be
		// able to perform matches as the parameters are specified in the encoded password.
		final String registrationId = "argon2";

		final Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
		encoders.put(registrationId, argon());

		final DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(registrationId, encoders);
		encoder.setDefaultPasswordEncoderForMatches(PasswordEncoderFactories.createDelegatingPasswordEncoder());
		return encoder;
	}
}
