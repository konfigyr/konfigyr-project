package com.konfigyr.crypto;

import com.konfigyr.io.ByteArray;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * Interface that defines cryptographic operations that can be performed against a {@link Keyset}.
 * <p>
 * It is advised to use {@link KeysetOperationsFactory} to create new instances of this type
 * as the factory would make sure that the supplied {@link Keyset} is present in the {@link KeysetStore}.
 *
 * @author : Vladimir Spasic
 * @since : 27.03.24, Wed
 * @see Keyset
 * @see KeysetOperationsFactory
 **/
public sealed interface KeysetOperations permits KonfigyrKeysetOperations {

	/**
	 * Create a new {@link KeysetOperations} instance that would use the given {@link Keyset}
	 * to delegate cryptographic operations.
	 *
	 * @param keyset keyset that would perform cryptographic operations, can't be {@literal null}.
	 * @return the keyset operations, never {@literal null}.
	 */
	static @NonNull KeysetOperations of(@NonNull Keyset keyset) {
		return of(() -> keyset);
	}

	/**
	 * Create a new {@link KeysetOperations} instance that would use the given {@link Keyset}
	 * supplier to delegate cryptographic operations.
	 *
	 * @param keyset supplier that would provide a keyset to perform cryptographic operations,
	 *               can't be {@literal null}.
	 * @return the keyset operations, never {@literal null}.
	 */
	static @NonNull KeysetOperations of(@NonNull Supplier<Keyset> keyset) {
		return new KonfigyrKeysetOperations(keyset);
	}

	/**
	 * Encrypt the given byte buffer.
	 *
	 * @param data Data wrapped as a byte buffer that should be encrypted, can't be {@literal null}.
	 * @return Encrypted data wrapped inside a byte buffer, never {@literal null}.
	 * @throws CryptoException in case encrypt operation can not be performed
	 */
	@NonNull
	ByteArray encrypt(@NonNull ByteArray data);

	/**
	 * Encrypt the byte buffer with an additional data to be used as authentication
	 * context when performing encryption.
	 *
	 * @param data    Data wrapped as a byte buffer that should be encrypted, can't be {@literal null}.
	 * @param context Authentication context byte bugger, can be {@literal null}.
	 * @return Encrypted data wrapped inside a byte buffer, never {@literal null}.
	 * @throws CryptoException in case encrypt operation can not be performed
	 */
	@NonNull
	ByteArray encrypt(@NonNull ByteArray data, @Nullable ByteArray context);

	/**
	 * Decrypt the given byte buffer.
	 *
	 * @param cipher Data wrapped as a byte buffer that should be decrypted, can't be {@literal null}.
	 * @return Decrypted data wrapped inside a byte buffer, never {@literal null}.
	 * @throws CryptoException in case decrypt operation can not be performed
	 */
	@NonNull
	ByteArray decrypt(@NonNull ByteArray cipher);

	/**
	 * Decrypt the byte buffer with an additional data was used during encryption as
	 * authentication context.
	 *
	 * @param cipher  Data wrapped as a byte buffer that should be decrypted, can't be {@literal null}.
	 * @param context Authentication context byte bugger, can be {@literal null}.
	 * @return Decrypted data wrapped inside a byte buffer, never {@literal null}.
	 * @throws CryptoException in case decrypt operation can not be performed
	 */
	@NonNull
	ByteArray decrypt(@NonNull ByteArray cipher, @Nullable ByteArray context);

	/**
	 * Signs the data wrapped inside a {@link ByteArray}. This method would return a
	 * {@link ByteArray} that contains the digital signature.
	 *
	 * @param data Data wrapped as a byte buffer that should be signed, can't be {@literal null}.
	 * @return digital signature wrapped inside a byte buffer, never {@literal null}.
	 * @throws CryptoException in case signing operation can not be performed
	 */
	@NonNull
	ByteArray sign(@NonNull ByteArray data);

	/**
	 * Verifies if digital signature of the data wrapped inside a {@link ByteArray} is correct.
	 *
	 * @param signature Signature wrapped as a byte buffer that should be verified, can't be {@literal null}.
	 * @param data      Original data wrapped as a byte buffer from which the signature is created, can't be {@literal null}.
	 * @return {@code true} if the signature is valid, {@code false} otherwise.
	 * @throws CryptoException in case signature verification operation can not be performed
	 */
	boolean verify(@NonNull ByteArray signature, @NonNull ByteArray data);

}
