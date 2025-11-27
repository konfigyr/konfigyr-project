package com.konfigyr.identity.authorization.jwk;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.function.SingletonSupplier;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Utility class that selects a signing JWK from a collection of JWKs based on the {@link KeyOperation#SIGN} operation.
 * <p>
 * The {@link KeysetSource} uses the JOSE keysets that are making sure that only the primary keys are able
 * to perform signing operations. For this reason, we can be safe and select the primary signing key based on
 * the supported operations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class SigningJwkSelector {

	private static final Supplier<SigningJwkSelector> INSTANCE = SingletonSupplier.of(SigningJwkSelector::new);

	public static SigningJwkSelector getInstance() {
		return INSTANCE.get();
	}

	@Nullable
	public JWK select(@NonNull Iterable<JWK> keys) {
		for (JWK key : keys) {
			final Collection<KeyOperation> operations = key.getKeyOperations();

			if (operations != null && operations.contains(KeyOperation.SIGN)) {
				return key;
			}
		}
		return null;
	}
}
