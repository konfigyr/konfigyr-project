package com.konfigyr.support;

import io.hypersistence.tsid.TSID;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.util.function.SingletonSupplier;

import java.util.function.Supplier;

/**
 * Implementation of the {@link StringKeyGenerator} that would use the {@link TSID} to generate
 * unique time sorted keys that are Base 16 encoded.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TSID
 * @see StringKeyGenerator
 **/
public class KeyGenerator implements StringKeyGenerator {

	private static final Supplier<KeyGenerator> INSTANCE = SingletonSupplier.of(KeyGenerator::new);

	@NonNull
	public static KeyGenerator getInstance() {
		return INSTANCE.get();
	}

	@NonNull
	@Override
	public String generateKey() {
		return TSID.fast().encode(16);
	}

}
