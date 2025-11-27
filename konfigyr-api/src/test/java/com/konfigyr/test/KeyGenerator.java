package com.konfigyr.test;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.util.Lazy;
import org.springframework.util.function.SingletonSupplier;

import java.util.function.Supplier;

@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyGenerator implements Supplier<JWK> {

	private static final Supplier<KeyGenerator> instance = SingletonSupplier.of(KeyGenerator::new);

	public static KeyGenerator getInstance() {
		return instance.get();
	}

	private final JWSSignerFactory signerFactory = new DefaultJWSSignerFactory();
	private final Lazy<JWK> key = Lazy.of(this::generate);

	/**
	 * Obtains the current {@link JWK RSA Signing Key} for JWT based authentications.
	 *
	 * @return the current RSA key, never {@literal null}
	 */
	@Override
	public JWK get() {
		return key.get();
	}

	/**
	 * Generates a new {@link JWK RSA Signing Key} for JWT based authentications.
	 *
	 * @return the RSA key, never {@literal null}
	 * @throws IllegalArgumentException when RSA key can not be generated
	 */
	public JWK generate() {
		try {
			return new RSAKeyGenerator(2048)
					.algorithm(JWSAlgorithm.RS256)
					.keyUse(KeyUse.SIGNATURE)
					.generate();
		} catch (JOSEException ex) {
			throw new IllegalArgumentException("Could not generate signing key", ex);
		}
	}

	/**
	 * Signs the {@link JWTClaimsSet JWT Claims} using the current {@link JWK RSA Signing Key}.
	 *
	 * @param claims claims to be signed, never {@literal null}
	 * @return the {@link JWSObject signed JWT}, never {@literal null}
	 */
	public JWSObject sign(JWTClaimsSet claims) {
		return sign(get(), claims);
	}

	/**
	 * Signs the {@link JWTClaimsSet JWT Claims} using the given {@link JWK RSA Signing Key}.
	 *
	 * @param key signing JWT key, never {@literal null}
	 * @param claims claims to be signed, never {@literal null}
	 * @return the {@link JWSObject signed JWT}, never {@literal null}
	 */
	public JWSObject sign(JWK key, JWTClaimsSet claims) {
		final JWSSigner signer;

		try {
			signer = signerFactory.createJWSSigner(key, JWSAlgorithm.RS256);
		} catch (JOSEException ex) {
			throw new IllegalStateException("Could not generate signer for key: " + key, ex);
		}

		final var object = new JWSObject(new JWSHeader(JWSAlgorithm.RS256), claims.toPayload());

		try {
			object.sign(signer);
		} catch (JOSEException ex) {
			throw new IllegalStateException("Could not sign clams using key: " + key, ex);
		}

		return object;
	}

}
