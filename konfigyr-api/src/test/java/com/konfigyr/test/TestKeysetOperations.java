package com.konfigyr.test;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKey;
import com.google.crypto.tink.aead.AesGcmParameters;
import com.google.crypto.tink.util.SecretBytes;
import com.konfigyr.crypto.*;
import com.konfigyr.crypto.tink.TinkAlgorithm;
import com.konfigyr.crypto.tink.TinkKeyEncryptionKey;
import com.konfigyr.crypto.tink.TinkKeysetFactory;
import com.konfigyr.io.ByteArray;
import org.jspecify.annotations.NonNull;

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

			final KeysetHandle handle = KeysetHandle.newBuilder()
					.addEntry(KeysetHandle.importKey(key).makePrimary())
					.build();

			final KeyEncryptionKey kek = TinkKeyEncryptionKey.builder("test-provider").from("test-kek", handle);
			final EncryptedKeyset encryptedKeyset = EncryptedKeyset.builder(definition)
					.keyEncryptionKey(kek)
					.build(new ByteArray(TinkProtoKeysetFormat.serializeEncryptedKeyset(
							handle, handle.getPrimitive(RegistryConfiguration.get(), Aead.class),
							null
					)));

			return KeysetOperations.of(new TinkKeysetFactory().create(kek, encryptedKeyset));
		} catch (Exception ex) {
			throw new IllegalStateException("Unexpected error occurred while creating testing Keyset operations", ex);
		}
	}

	private TestKeysetOperations() {
	}
}
