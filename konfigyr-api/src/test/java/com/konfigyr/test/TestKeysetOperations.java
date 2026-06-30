package com.konfigyr.test;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKey;
import com.google.crypto.tink.aead.AesGcmParameters;
import com.google.crypto.tink.internal.MutableSerializationRegistry;
import com.google.crypto.tink.proto.KeyData;
import com.google.crypto.tink.util.SecretBytes;
import com.konfigyr.crypto.*;
import com.konfigyr.crypto.KeyStatus;
import com.konfigyr.crypto.tink.TinkAlgorithm;
import com.konfigyr.crypto.tink.TinkKeyEncryptionKey;
import com.konfigyr.crypto.tink.TinkKeysetFactory;
import com.konfigyr.io.ByteArray;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * Utility class used to create an instance of {@link KeysetOperations} for testing purposes.
 * <p>
 * The generated {@link KeysetOperations} would be using the {@link Keyset} that uses the
 * {@link TinkAlgorithm#AES128_GCM} algorithm and a random {@link TinkKeyEncryptionKey}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class TestKeysetOperations {

	/**
	 * Creates a {@link KeysetOperations} instance for testing purposes using the random generated secret key.
	 *
	 * @return the keyset operations, never {@literal null}.
	 * @throws IllegalStateException if the {@link KeysetOperations} can't be created for some reason
	 */
	@NonNull
	public static KeysetOperations create() {
		return create(SecretBytes.randomBytes(32).toByteArray(InsecureSecretKeyAccess.get()));
	}

	/**
	 * Creates a {@link KeysetOperations} instance for testing purposes using the supplied secret key.
	 *
	 * @param secret the secret key to use, must not be {@literal null}.
	 * @return the keyset operations, never {@literal null}.
	 * @throws IllegalStateException if the {@link KeysetOperations} can't be created for some reason
	 */
	@NonNull
	public static KeysetOperations create(byte[] secret) {
		return create(new ByteArray(secret));
	}

	/**
	 * Creates a {@link KeysetOperations} instance for testing purposes using the supplied secret key.
	 *
	 * @param secret the secret key to use, must not be {@literal null}.
	 * @return the keyset operations, never {@literal null}.
	 * @throws IllegalStateException if the {@link KeysetOperations} can't be created for some reason
	 */
	@NonNull
	public static KeysetOperations create(ByteArray secret) {
		try {
			AeadConfig.register();

			final SimpleAlgorithmRegistry registry = new SimpleAlgorithmRegistry();
			registry.register(TinkAlgorithm.AES128_GCM);

			final KeysetDefinition definition = KeysetDefinition.of("test-keyset", TinkAlgorithm.AES128_GCM);

			final AesGcmKey key = AesGcmKey.builder()
					.setKeyBytes(SecretBytes.copyFrom(secret.array(), InsecureSecretKeyAccess.get()))
					.setParameters(AesGcmParameters.builder()
							.setVariant(AesGcmParameters.Variant.TINK)
							.setKeySizeBytes(secret.size())
							.setTagSizeBytes(16)
							.setIvSizeBytes(12)
							.build())
					.setIdRequirement(1)
					.build();

			final KeyEncryptionKey kek = TinkKeyEncryptionKey.builder("test-provider")
					.from("test-kek", secret);

			final ProtoKeySerialization serialization = MutableSerializationRegistry.globalInstance()
					.serializeKey(key, InsecureSecretKeyAccess.get());

			final KeyData data = KeyData.newBuilder()
					.setTypeUrl(serialization.getTypeUrl())
					.setKeyMaterialType(KeyData.KeyMaterialType.valueOf(serialization.getKeyMaterialType().toString()))
					.setValue(serialization.getValue())
					.build();

			final EncryptedKey encryptedKey = EncryptedKey.builder()
					.id("94323357")
					.primary(true)
					.status(KeyStatus.ENABLED)
					.algorithm(TinkAlgorithm.AES128_GCM)
					.createdAt(Instant.now())
					.build(kek.wrap(new ByteArray(data.toByteArray())));

			final EncryptedKeyset encryptedKeyset = EncryptedKeyset.builder(definition)
					.keyEncryptionKey(kek)
					.build(encryptedKey);

			return KeysetOperations.of(new TinkKeysetFactory(registry).create(kek, encryptedKeyset));
		} catch (Exception ex) {
			throw new IllegalStateException("Unexpected error occurred while creating testing Keyset operations", ex);
		}
	}

	private TestKeysetOperations() {
	}
}
